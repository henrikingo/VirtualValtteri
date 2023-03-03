package cc.openlife.virtualvaltteri;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.PowerManager;
import android.net.Uri;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;

import cc.openlife.virtualvaltteri.speaker.Speaker;
import cc.openlife.virtualvaltteri.speaker.VeryShort;
import cc.openlife.virtualvaltteri.vmkarting.MessageHandler;
public class MainActivity extends AppCompatActivity {
    // Get WebSocket URL from properties file
    String websocketUrl = null;
    String testRun = "false";
    float ttsPitch = 1.0F;
    String ttsVoice = "default";
    private WebSocketManager mWebSocket;
    private TextView mTextView;
    private TextView mTextViewLarge;
    private TextView mTextViewCarNr;
    private TextView mTextViewPosition;
    private MessageHandler handler;
    private TextToSpeech tts;
    public Set<String> followDriverNames;
    Set<String> followDriverIds = new HashSet<String>(Collections.emptyList());
    private final String initialMessage = "Valtteri. It's James.\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Creating main app activity...");
        // Initialize UI elements
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.text_view);
        mTextViewLarge = findViewById(R.id.idLargeText);
        mTextViewCarNr = findViewById(R.id.idCarNr);
        mTextViewPosition = findViewById(R.id.idPosition);

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars.
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        Button settingsBtn = findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // opening a new intent to open settings activity.
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                // Send list of drivers in this race...
                SortedSet<String> sortedDrivers = new ConcurrentSkipListSet<>(handler.driverIdLookup.keySet());
                ArrayList<CharSequence> samesamebutdifferent = new ArrayList<>();
                for(String s: sortedDrivers){
                    samesamebutdifferent.add((CharSequence) s);
                }
                //samesamebutdifferent.add("Kekekekek");
                //samesamebutdifferent.add("Rosberg");
                System.out.println("Putting driverlist in the intent sending to settings: " + samesamebutdifferent);
                intent.putCharSequenceArrayListExtra("sortedDrivers", samesamebutdifferent);
                //  TODO: Fix deprecated
                startActivityForResult(intent, 1);
            }
        });
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences" ,Context.MODE_PRIVATE);
        followDriverIds = prefs.getStringSet("drivers_key", new HashSet<String>(Collections.emptyList()));
        System.out.println("Recovered followDriverIds from shared preferences storage: " + followDriverIds);
        followDriverNames = prefs.getStringSet("drivers_key", new HashSet<String>(Arrays.asList()));
        String speaker = prefs.getString("speaker_key", null);

        handler = new MessageHandler(this.followDriverNames);
        if(speaker!=null) {
            switch (speaker) {
                case "Speaker":
                    System.out.println("Switch to (default) Speaker speaker mode.");
                    handler.speaker = new Speaker();
                    break;
                case "VeryShort":
                    System.out.println("Switch to VeryShort speaker mode.");
                    handler.speaker = new VeryShort();
                    break;
            }
        }


        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            websocketUrl = properties.getProperty("websocket.url");
            testRun = properties.getProperty("testrun");
            ttsPitch = Float.parseFloat(properties.getProperty("tts.pitch", "" + ttsPitch));
            ttsVoice = properties.getProperty("tts.voice", ttsVoice);
            // This is the main audio stream managed by your hardware up-down key
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (IOException e) {
            System.out.println("Failed to read properties file: " + e.getMessage());
        }

        // Run also when in background
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        if (!pm.isIgnoringBatteryOptimizations(packageName)){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            });
        }


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

    protected String cutDecimal(String d){
        int dot = d.indexOf(".");
        if (dot >= 1){
            return d.substring(0,dot+2);
        }
        return d;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("MainActivity.onActivityResult() " +requestCode + " " + resultCode + " " + data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                String speaker = data.getStringExtra("settings_speaker");
                if(speaker!=null){
                    switch (speaker) {
                        case "Speaker":
                            System.out.println("Switch to (default) Speaker speaker mode.");
                            handler.speaker = new Speaker();
                            break;
                        case "VeryShort":
                            System.out.println("Switch to VeryShort speaker mode.");
                            handler.speaker = new VeryShort();
                            break;
                    }
                }
                ArrayList<CharSequence> driversCS = data.getCharSequenceArrayListExtra("settings_drivers");
                if(driversCS!=null && driversCS.size()>0){
                    followDriverNames.clear();
                    for(CharSequence cs: driversCS){
                        followDriverNames.add((String) cs);
                    }
                    System.out.println("onActivityResult() followDriverNames" + followDriverNames);
                }
            }
        }
    }
    public void ttsDone() {
        mTextView = findViewById(R.id.text_view);
        tts.speak(initialMessage, TextToSpeech.QUEUE_ADD, null, null);
        AssetManager assetManager = getAssets();

//        System.out.println("testrun is: "+testRun);
        if (testRun.startsWith("true")) {
            System.out.println("Doing test run with test data, no network connections created.");
            try {
                InputStream testDataInputStream = assetManager.open("testdata.txt");
                BufferedReader testDataReader = new BufferedReader(new InputStreamReader(testDataInputStream));
                String line = null;
                try {
                    while ((line = testDataReader.readLine()) != null) {
                        if (line.equals(""))
                            continue;
//                               System.out.println("first line: " + line);

                        StringBuilder message = new StringBuilder();
                        message.append(line).append("\n");
                        while ((line = testDataReader.readLine()) != null && !line.equals("")) {
//                                   System.out.println("multiline: " + line);
                            message.append(line).append("\n");
                        }
                        Map<String,String> englishMessageMap = handler.message(message.toString());
                        String englishMessage = englishMessageMap.get("message");
                        if(englishMessageMap.containsKey("driverChanged"))
                            englishMessage += "\n";
                        if (!(englishMessage.equals(""))) {
                            tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
                            String finalEnglishMessage = englishMessage;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextView.setText(finalEnglishMessage);
                                    if(englishMessageMap.containsKey("s1"))
                                        mTextViewLarge.setText(cutDecimal(englishMessageMap.get("s1")));
                                    if(englishMessageMap.containsKey("s2"))
                                        mTextViewLarge.setText(cutDecimal(englishMessageMap.get("s2")));
                                    if(englishMessageMap.containsKey("lap"))
                                        mTextViewLarge.setText(cutDecimal(englishMessageMap.get("lap")));
                                    if(englishMessageMap.containsKey("position"))
                                        mTextViewPosition.setText("P"+englishMessageMap.get("position"));
                                    if(englishMessageMap.containsKey("carNr"))
                                        mTextViewCarNr.setText(englishMessageMap.get("carNr"));
                                    if(englishMessageMap.containsKey("time_meta")){
                                        if(englishMessageMap.get("time_meta").equals("improved"))
                                            setColorAll(getColor(R.color.timeImproved));
                                        else if(englishMessageMap.get("time_meta").equals("individual best"))
                                            setColorAll(getColor(R.color.timeIndividualBest));
                                        else if(englishMessageMap.get("time_meta").equals("best"))
                                            setColorAll(getColor(R.color.timeBest));
                                        else
                                            setColorAll(getColor(R.color.timeNormal));
                                    }
                                }
                            });

                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        testDataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Connect to WebSocket server
        if (websocketUrl != null) {
            connectWebsocket();
        }
    }
    private void setColorAll(int color){
        if(mTextViewPosition==null)
            return;


        mTextViewPosition.setTextColor(color);
        mTextViewCarNr.setTextColor(color);
        mTextViewLarge.setTextColor(color);
    }
    public void connectWebsocket(){
        try {
            this.mWebSocket = new WebSocketManager(websocketUrl, this);
            this.mWebSocket.connect();
        }
        catch(URISyntaxException ex) {
            System.err.println("Server URI is wrongly formatted: " + ex);
            System.err.println("Can't really do much without the websocket. Giving up. ");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close WebSocket connection
        if (mWebSocket != null) {
            mWebSocket.close();
            mWebSocket = null;
        }
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
    }

    public void processMesssage(String message) {
        Map<String,String> englishMessageMap = handler.message(message);
        String englishMessage = englishMessageMap.get("message");
        String messageType = englishMessageMap.get("type");
        if(! (englishMessage.equals(""))) {
            tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(mTextView!=null)
                        mTextView.setText(mTextView.getText() + " " + englishMessage);
                    if(englishMessageMap.containsKey("s1") && mTextViewLarge!=null)
                        mTextViewLarge.setText(cutDecimal(englishMessageMap.get("s1")));
                    if(englishMessageMap.containsKey("s2") && mTextViewLarge!=null)
                        mTextViewLarge.setText(cutDecimal(englishMessageMap.get("s2")));
                    if(englishMessageMap.containsKey("lap") && mTextViewLarge!=null)
                        mTextViewLarge.setText(cutDecimal(englishMessageMap.get("lap")));
                    if(englishMessageMap.containsKey("carNr") && mTextViewCarNr!=null) {
                        if (!(englishMessageMap.get("carNr").equals(mTextViewCarNr.getText())))
                            // Reset the position field since we don't know the position of the new car
                            mTextViewPosition.setText("");
                        mTextViewCarNr.setText(englishMessageMap.get("carNr"));
                    }
                    // Of course if we have the position, great :-)
                    if(englishMessageMap.containsKey("position") && mTextViewPosition!=null)
                        mTextViewPosition.setText("P"+englishMessageMap.get("position"));
                    if(englishMessageMap.containsKey("time_meta")){
                        if(englishMessageMap.get("time_meta").equals("improved"))
                            setColorAll(getColor(R.color.timeImproved));
                        else if(englishMessageMap.get("time_meta").equals("individual best"))
                            setColorAll(getColor(R.color.timeIndividualBest));
                        else if(englishMessageMap.get("time_meta").equals("best"))
                            setColorAll(getColor(R.color.timeBest));
                        else
                            setColorAll(getColor(R.color.timeNormal));
                    }
               }
            });
        }
    }
}
