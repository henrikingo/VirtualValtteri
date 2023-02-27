package cc.openlife.virtualvaltteri;

import android.app.Activity;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
    private MessageHandler handler;
    private TextToSpeech tts;
    public Set<String> followDriverNames;
    Set<String> followDriverIds = new HashSet<String>(Collections.emptyList());
    private final String initialMessage = "Valtteri. It's James.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Creating main app activity...");
        // Initialize UI elements
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.text_view);
        // Note that driver Ids are only valid for the day
        SharedPreferences prefs = getSharedPreferences("VirtualValtteri", 0);
        followDriverIds = prefs.getStringSet("followDriverIds", new HashSet<String>(Collections.emptyList()));
        String driverIdDate = prefs.getString("driverIdDate", "1970-01-01");
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = df.format(c);
        if(!driverIdDate.equals(formattedDate)){
            SharedPreferences.Editor prefsEdit = prefs.edit();
            followDriverIds = new HashSet<>(Collections.emptyList());
            prefsEdit.putStringSet("followDriverIds", followDriverIds);
            driverIdDate = formattedDate;
            prefsEdit.putString("driverIdDate", formattedDate);
            prefsEdit.apply();
        }
        Button settingsBtn = findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // opening a new intent to open settings activity.
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                // Send list of drivers in this race...
                ArrayList<String> sortedDrivers = new ArrayList<>(handler.driverIdLookup.keySet());
                Collections.sort(sortedDrivers);
                ArrayList<CharSequence> samesamebutdifferent = new ArrayList<>();
                for(String s: sortedDrivers){
                    samesamebutdifferent.add((CharSequence) s);
                }
                System.out.println("Putting driverlist in the intent sending to settings: " + sortedDrivers);
                intent.putCharSequenceArrayListExtra("sortedDrivers", samesamebutdifferent);
                ///startActivity(intent);
                //  TODO: Fix deprecated
                startActivityForResult(intent, 1);
            }
        });

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
        followDriverNames = prefs.getStringSet("followDrivers", new HashSet<String>(Arrays.asList()));
        handler = new MessageHandler(this.followDriverNames);

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
                        String englishMessage = handler.message(message.toString());
                        if (!(englishMessage.equals(""))) {
                            tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextView.setText(englishMessage);
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
    public void connectWebsocket(){
        try {
            this.mWebSocket = new WebSocketManager(websocketUrl, this);
            this.mWebSocket.connect();
        }
        catch(URISyntaxException ex) {
            System.err.println("Server URI is wrongly formatted: " + ex);
            System.err.println("Can't really do much without the websocket. Giving up.");
        }
    }

    public void processMessage(String message){
        String englishMessage = handler.message(message);
        if(! (englishMessage.equals(""))) {
            tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(englishMessage);
                }
            });
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
        String englishMessage = handler.message(message);
        if(! (englishMessage.equals(""))) {
            tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(englishMessage);
                }
            });
        }
    }
}
