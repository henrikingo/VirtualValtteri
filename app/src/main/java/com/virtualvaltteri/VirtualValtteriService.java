package com.virtualvaltteri;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.tts.TextToSpeech;

import com.virtualvaltteri.sensors.Collect;
import com.virtualvaltteri.speaker.Quiet;
import com.virtualvaltteri.speaker.Speaker;
import com.virtualvaltteri.speaker.VeryShort;
import com.virtualvaltteri.vmkarting.MessageHandler;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;

public class VirtualValtteriService extends Service {
    public int startId=-1;
    String websocketUrl = null;
    private WebSocketManager mWebSocket;
    String testRun = "false";
    public Collect collect;
    private MessageHandler handler;
    private TextToSpeech tts;
    float ttsPitch = 1.0F;
    String ttsVoice = "default";
    public Set<String> followDriverNames;
    Set<String> followDriverIds = new HashSet<>(Collections.emptyList());
    private final String initialMessage = "Valtteri. It's James.\n";
    private final IBinder binder = new VirtualValtteriBinder();
    private final List<Map<String,String>> pastMessages = Collections.synchronizedList(new ArrayList<>(100));
    private Handler ui;

    public class VirtualValtteriBinder extends Binder {
        VirtualValtteriService getService() {
            return VirtualValtteriService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public List<Map<String,String>> getPastMessages(){
        return pastMessages;
    }
    public boolean subscribe(Handler handler){
        ui = handler;
        return true;
    }

    public boolean unsubscribe(Handler handler){
        if (ui==handler){
            ui = null;
            return true;
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (this.startId >= 0 && this.startId != startId){
            System.err.println(String.format("VirtualValtteriService: I received a different startId (%s) than the one I already have (%s)", startId, this.startId));
        }
        this.startId=startId;
        this.collect = Collect.getInstance(getApplicationContext());
        // Hurry up with that sticky notification
        if (this.collect.notification == null)
            this.collect.standby(this);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        this.followDriverNames = prefs.getStringSet("follow_driver_names_key", new HashSet<>());
        this.collect.VVS = this;
        dispatchBundleCommands(intent);

        return START_REDELIVER_INTENT;
    }

    private void dispatchBundleCommands(Intent intent) {
        String type = intent.getStringExtra("type");
        String doWhat = intent.getStringExtra("do");
        System.out.println("type: " + type + "    do:" + doWhat);

        if(type.equals("com.virtualvaltteri.VirtualValtteriService")){
            if(doWhat.equals("start")){
                applyPreferences();
                startWebsocketManager();
            }
        }
        if(type.equals("com.virtualvaltteri.VirtualValtteriService.sensors")){
            if(doWhat.equals("start")){
                applyPreferences();
                collect.startSensors();
            }
            if(doWhat.equals("stop")){
                collect.stopSensors();
            }
        }
        if(type.equals("com.virtualvaltteri.VirtualValtteriService.race")){
            if(doWhat.equals("start")){
                collect.raceStarted();
            }
            if(doWhat.equals("stop")){
                collect.raceStopped();
            }
        }
        if(type.equals("com.virtualvaltteri.VirtualValtteriService.preferences")){
            if(doWhat.equals("apply")){
                applyPreferences();
            }
        }
    }

    public void processMessage(String message) {
        Map<String,String> englishMessageMap = handler.message(message);
        String englishMessage = englishMessageMap.get("message");
        String messageType = englishMessageMap.get("type");
        if(! (englishMessage.equals(""))) {
            tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
        }

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        SortedSet<String> sortedDrivers = new ConcurrentSkipListSet<>(handler.driverIdLookup.keySet());
        prefs.edit().putStringSet("sorted_drivers_key", ((Set)sortedDrivers)).commit();

        sendToMain("com.virtualvaltteri.processMessage", englishMessageMap);

       //if(englishMessageMap!=null)
         //   q.add(englishMessageMap);

        if(ui!=null){
            Message msg = new Message();
            msg.obj = englishMessageMap;
            ui.sendMessage(msg);
        }
        if (messageType=="init") pastMessages.clear();
        pastMessages.add(englishMessageMap);
    }

    public void sendToMain(CharSequence type, Map<String,String> englishMessageMap){
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("type", type);
        ArrayList<String> keys = new ArrayList<String>(englishMessageMap.keySet());
        mainIntent.putStringArrayListExtra("keys", keys);
        ArrayList<String> values = new ArrayList<>();
        for (int i=0; i<keys.size(); i++) {
            values.add(englishMessageMap.get(keys.get(i)));
        }
        mainIntent.putStringArrayListExtra("values", values);
        //startActivity(mainIntent);

        String englishMessage = englishMessageMap.get("message");
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String largeMessage = prefs.getString("large_message_key", "Valtteri, it's James.");
        editor.putString("english_message_ui", largeMessage + englishMessage);
        editor.commit();

    }

    public void connectWebsocket(){
        if(this.mWebSocket == null){
            try {
                this.mWebSocket = new WebSocketManager(websocketUrl, this);
            }
            catch(URISyntaxException ex) {
                System.err.println("Server URI is wrongly formatted: " + ex);
                System.err.println("Can't really do much without the websocket. Giving up. ");
            }
        }
        this.mWebSocket.connect();
    }
    private void startWebsocketManager(){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        String speaker = prefs.getString("speaker_key", null);
        assert this.followDriverNames != null;
        setSpeaker(prefs);

        websocketUrl = getString(R.string.url);
        testRun = getString(R.string.testrun);

        ttsPitch = Float.parseFloat(getString(R.string.ttspitch));
        ttsVoice = getString(R.string.ttsvoice);

        float finalTtsPitch = ttsPitch;
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.UK);
                    tts.setPitch(finalTtsPitch);
                    //tts.setVoice(new Voice(...)); // https://developer.android.com/reference/android/speech/tts/Voice
                    ttsDone();
                }
            }
        });
    }

    public void ttsDone() {
        tts.speak(initialMessage, TextToSpeech.QUEUE_ADD, null, null);
        // Connect to WebSocket server
        connectWebsocket();
    }

    public void applyPreferences() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        collect.startStopSensors();
        setDrivers(prefs);
        if (handler==null) handler = new MessageHandler(this.followDriverNames, this.collect);
        else handler.followDriverNames = prefs.getStringSet("follow_driver_names_key", new HashSet<>());

        SortedSet<String> sortedDrivers = new ConcurrentSkipListSet<>(handler.driverIdLookup.keySet());
        prefs.edit().putStringSet("sorted_drivers_key", ((Set)sortedDrivers));


        setSpeaker(prefs);
        System.out.println("VVS.applyPreferences()");
    }
    private void setSpeaker(SharedPreferences prefs) {
        String speaker = prefs.getString("speaker_key", "Speaker");
        assert handler != null;

        if (handler.speaker == null
            || speaker != null && !speaker.equals(handler.speaker.type)) {
            switch (speaker) {
                case "Speaker":
                    System.out.println("Switch to (default) Speaker speaker mode.");
                    handler.speaker = new Speaker();
                    break;
                case "VeryShort":
                    System.out.println("Switch to VeryShort speaker mode.");
                    handler.speaker = new VeryShort();
                    break;
                case "Quiet":
                    System.out.println("Switch to Quiet speaker mode.");
                    handler.speaker = new Quiet();
                    break;
            }
        }
    }
    private void setDrivers(SharedPreferences prefs){
        followDriverIds = prefs.getStringSet("drivers_key", new HashSet<String>(Collections.emptyList()));
        System.out.println("Recovered followDriverIds from shared preferences storage: " + followDriverIds);
        followDriverNames = prefs.getStringSet("drivers_key", new HashSet<String>(Arrays.asList()));
    }
    @Override
    public void onDestroy() {
        collect.stopSensors();
        collect.stopNotificationTimer();
        super.onDestroy();
    }
}