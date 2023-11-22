package com.virtualvaltteri;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.tts.TextToSpeech;

import androidx.preference.MultiSelectListPreference;

import com.virtualvaltteri.sensors.Collect;
import com.virtualvaltteri.settings.DynamicMultiSelectListPreference;
import com.virtualvaltteri.settings.SettingsFragment;
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
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;

public class VirtualValtteriService extends Service {
    public boolean shutdown=false;
    private static VirtualValtteriService VvsInstance=null;
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
    private String initialMessage = "Valtteri. It's James.\n";
    private int initialMessageDone = 2;
    private final IBinder binder = new VirtualValtteriBinder();
    private final List<Map<String,String>> pastMessages = Collections.synchronizedList(new ArrayList<>(100));
    private Handler ui;
    private boolean refreshing = false;

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
        VvsInstance = this;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand " + startId);
        if(shutdown){
            //System.exit(-1);
            System.out.println("onStartCommand: shutdown initiated, returning immediately.");
            System.exit(3);
            return Service.START_NOT_STICKY;
        }
        if (this.startId >= 0 && this.startId != startId){
            System.err.println(String.format("VirtualValtteriService: I received a different startId (%s) than the one I already have (%s)", startId, this.startId));
        }
        this.startId=startId;
        this.collect = Collect.getInstance(getApplicationContext());
        this.collect.startVvsEventLoop(this);
        // Hurry up with that sticky notification
        if (this.collect.notification == null)
            this.collect.standby(this);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        this.followDriverNames = prefs.getStringSet("follow_driver_names_key", new HashSet<>());
        dispatchBundleCommands(intent);
        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                System.out.println("VVS.onSharedPreferenceChanged");
                if(refreshing){
                    System.out.println("skipping refresh, already done somewhere else");
                }
                refreshing = true;
                applyPreferences();
                refreshing = false;
            }
        });

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
        if(type.equals("com.virtualvaltteri.VirtualValtteriService.foregroundPing")){
            if(doWhat.equals("ping")){
                collect.ping();
            }
        }
        if(type.equals("com.virtualvaltteri.VirtualValtteriService.system")){
            if(doWhat.equals("close")){
                closeApp();

            }
        }
    }

    public int closeApp(){
        shutdown=true;
        collect.stopVvsService();
        MainActivity.mainActivityInstance.closeApp();
        System.exit(-1);
        return 0;
    }
    public void processMessage(String message) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        //setDrivers(prefs);
        //applyPreferences();
        //System.out.println("start of processmessage: " + followDriverNames);

        Map<String,String> englishMessageMap = handler.message(message);
        String englishMessage = englishMessageMap.get("message");
        String messageType = englishMessageMap.get("type");
        if(! (englishMessage.equals(""))) {
            tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
        }

        SortedSet<String> sortedDrivers = new ConcurrentSkipListSet<>(handler.driverIdLookup.keySet());
  //      followDriverNames = DynamicMultiSelectListPreference.getReducedValuesHelper(true,
 //               prefs.getStringSet("follow_driver_names_key", (Set)new HashSet<>()),
//                sortedDrivers.toArray(new CharSequence[0])
//        );
//        followDriverNames = DynamicMultiSelectListPreference.getReducedValuesHelper(true,
//                new HashSet<>(handler.driverIdLookup.values()),
//                sortedDrivers.toArray(new CharSequence[0])
//        );
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("sorted_drivers_key", ((Set)sortedDrivers));
        editor.commit();

        sendToMain("com.virtualvaltteri.processMessage", englishMessageMap, (Set)sortedDrivers);
        //applyPreferences();
       //if(englishMessageMap!=null)
         //   q.add(englishMessageMap);

        if(ui!=null){
            Message msg = new Message();
            msg.obj = englishMessageMap;
            ui.sendMessage(msg);
        }
        if (messageType=="init") pastMessages.clear();
        pastMessages.add(englishMessageMap);
        //System.out.println("End of processmessage:" + followDriverNames);
    }

    public void sendToMain(CharSequence type, Map<String,String> englishMessageMap, Set sortedDriversLocal){
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
        editor.putStringSet("sorted_drivers_key", (Set)sortedDriversLocal);
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

    public void stopWebsocketManager(){
        if(this.mWebSocket!=null){
            this.mWebSocket.intentionallyClose();
        }
    }

    private String getInitialMessage(){
        // Just say initialMessage once when starting, never again:
        initialMessageDone--;
        if (initialMessageDone<0) initialMessage="";
        return initialMessage;
    }
    public void ttsDone() {
        tts.speak(getInitialMessage(), TextToSpeech.QUEUE_ADD, null, null);

        // Connect to WebSocket server
        connectWebsocket();
    }

    public void applyPreferences() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        collect.startStopSensors();
        setDrivers(prefs);
        if (handler==null) handler = new MessageHandler(this.followDriverNames, this.collect);
        else this.followDriverNames = prefs.getStringSet("follow_driver_names_key", new HashSet<>());

        SortedSet<String> sortedDrivers = new ConcurrentSkipListSet<>(handler.driverIdLookup.keySet());
        prefs.edit().putStringSet("sorted_drivers_key", ((Set)sortedDrivers)).commit();

        setSpeaker(prefs);
        updateDrivers(sortedDrivers);

        if(prefs.getString("system_close_key", "On").equals("Close")){
            System.out.println("VVS Closing VirtualValtteri. Reason: User selected Close in Settings.");
            closeApp();
            //System.exit(0);
        }


    }

    public static void updateDrivers (Set<String> sortedDrivers){
        SharedPreferences prefs =  VvsInstance.getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);

        CharSequence writeInName = prefs.getString("writein_driver_name_key", "");
        if(writeInName!=null)
            writeInName= ((String) writeInName).toLowerCase();
        boolean matchPrefix = prefs.getBoolean("match_writein_prefix_key",false);
        System.out.println("VVS.updateDrivers: writein name: " + writeInName);
        boolean autoFavorite = prefs.getBoolean("auto_favorite_key",false);

        //driversPreference = (DynamicMultiSelectListPreference) findPreference("drivers_key");
        //driversPreference.setEntries(sortedDrivers2);
        //driversPreference.setEntryValues(sortedDrivers2);
        Set<String> driversNotInThisSession = DynamicMultiSelectListPreference.getReducedValuesHelper(
                false,
                prefs.getStringSet("drivers_key", new HashSet<>()),
                sortedDrivers.toArray(new CharSequence[0])
        );
        Set<String> followDriversInThisSession = DynamicMultiSelectListPreference.getReducedValuesHelper(
                true,
                prefs.getStringSet("drivers_key", new HashSet<>()),
                sortedDrivers.toArray(new CharSequence[0])
        );
        Set<String> allDriversInThisSessionSet = (new HashSet<>());
        allDriversInThisSessionSet.addAll(driversNotInThisSession);
        allDriversInThisSessionSet.addAll(followDriversInThisSession);
        CharSequence[] allDriversInThisSession = allDriversInThisSessionSet.toArray(new CharSequence[0]);
        System.out.println(driversNotInThisSession);
        System.out.println(followDriversInThisSession);

        CharSequence writeInNameFound = SettingsFragment._matchDriver(sortedDrivers.toArray(new CharSequence[0]), writeInName, matchPrefix, true);
        System.out.println("VVS.update   Writein name in session: " +writeInNameFound);
        if( writeInName!=null && (!writeInName.equals("")) && writeInNameFound != null){
            System.out.println("Adding writein driver");
            followDriversInThisSession.add(writeInNameFound.toString());
        }

        // favoritedDriversPreference are saved values from driversPreference that aren't driving
        // in the current session. This preference is not persisted. It is essentially a helper or sidecar to the previous list.
        Set<String> favDrivers = prefs.getStringSet("favorited_drivers_key", new HashSet<>());

        // For whatever reason simple favDrivers.addAll(driversNotInThisSession) refused to work...
        SortedSet<String> newFavDrivers = new ConcurrentSkipListSet<>();
        for(String s: favDrivers){
            // System.out.println(s);
            newFavDrivers.add(s);
        }
        for(String s: driversNotInThisSession){
            //System.out.println(s);
            newFavDrivers.add(s);
        }

        System.out.println("VVS favDrivers: " + favDrivers);
        //favDrivers.addAll(driversNotInThisSession);
        // Add all old follows to favorites, and make them checked/on
       /* System.out.println(autoFavorite);
        if(autoFavorite){
            favoritedDriversPreference.setValues(newFavDrivers);
        }
        System.out.println(favoritedDriversPreference.getValues());
        // Otherwise add them to the list but leave them unchecked. They will eventually disappear if not checked.
        favoritedDriversPreference.setEntries((CharSequence[]) newFavDrivers.toArray(new CharSequence[0]));
        favoritedDriversPreference.setEntryValues((CharSequence[]) newFavDrivers.toArray(new CharSequence[0]));



        System.out.println(favoritedDriversPreference.getValues());

        System.out.println("newfavDrivers: " + " " + newFavDrivers);

        // In this list we remove items that are unselected
        CharSequence[] allEntries = favoritedDriversPreference.getEntryValues();
        Vector<CharSequence> prunedEntries = new Vector<CharSequence>();
        for(CharSequence cs:allEntries){
            if(newFavDrivers.contains(cs.toString())){
                prunedEntries.add(cs);
            }
        }
        System.out.println("pruned favorites: " + prunedEntries);
        if(prunedEntries.size()>0){
            favoritedDriversPreference.setEntries((CharSequence[]) prunedEntries.toArray(new CharSequence[0]));
            favoritedDriversPreference.setEntryValues((CharSequence[]) prunedEntries.toArray(new CharSequence[0]));
        }

        if(allDriversInThisSession!=null){
            for (CharSequence s: allDriversInThisSession){
                if(favDrivers.contains(s) && !followDriversInThisSession.contains(s)){
                    followDriversInThisSession.add(s.toString());
                }
            }
        }

        driversPreference.setValues(followDriversInThisSession);
        System.out.println("driversPreference.getValues "  +driversPreference.getValues());
*/

        prefs.edit().putStringSet("follow_driver_names_key", followDriversInThisSession).commit();
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
    private int outputThrottle=0;
    public void setDrivers(SharedPreferences prefs){

        //System.out.println("followDriverIdsbefore: " + followDriverIds);
        //System.out.println("followDriverNames before: " + followDriverNames);
        followDriverIds = prefs.getStringSet("drivers_key", new HashSet<String>(Collections.emptyList()));
        followDriverNames = prefs.getStringSet("follow_driver_names_key", new HashSet<String>(Arrays.asList()));
        //System.out.println("Recovered followDriverIds from shared preferences storage: " + followDriverIds);
        //System.out.println("Recovered followDriverNames from shared preferences storage: " + followDriverNames);
    }
    @Override
    public void onDestroy() {
        collect.stopSensors();
        collect.stopNotificationTimer();
        System.out.flush();
        super.onDestroy();
    }
}