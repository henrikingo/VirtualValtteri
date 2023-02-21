package cc.openlife.virtualvaltteri;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import java.util.prefs.PreferenceChangeEvent;
import java.util.stream.Collectors;

import cc.openlife.virtualvaltteri.vmkarting.MessageHandler;
public class MainActivity extends AppCompatActivity {
    // Get WebSocket URL from properties file
    String websocketUrl = null;
    String testRun = "false";
    float ttsPitch = 1.0F;
    String ttsVoice = "default";
    private WebSocketManager mWebSocket;
    private TextView mTextView;
    private MultiAutoCompleteTextView driverNamesDropDown;
    private MessageHandler handler;
    private TextToSpeech tts;
    Set<String> followDriverNames;
    private String initialMessage = "James it's Valtteri.";
    MyWebsocketListener webSocketListener = new MyWebsocketListener() {
        @Override
        public void onMessageReceived(final String message) {
            // Called when a message is received from the server
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String englishMessage = handler.message(message);
                    mTextView.setText(englishMessage);
                }
            });
        }
        public void onConnected(){System.out.println("Websocket connected");}
        public void onDisconnected(){System.out.println("Websocket disconnected");}
        public void onError(Exception ex){System.err.println("websocket error: " + ex);}
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize UI elements
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.text_view);
        driverNamesDropDown = findViewById(R.id.driverNamesDropDown);

        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            websocketUrl = properties.getProperty("websocket.url");
            testRun = properties.getProperty("testrun");
            ttsPitch = Float.parseFloat(properties.getProperty("tts.pitch", "" + ttsPitch));
            ttsVoice = properties.getProperty("tts.voice", ttsVoice);
        } catch (IOException e) {
            System.out.println("Failed to read properties file: " + e.getMessage());
        }
        SharedPreferences prefs = getSharedPreferences("VirtualValtteri", 0);
        followDriverNames = prefs.getStringSet("followDrivers", new HashSet<String>(Arrays.asList("albert", "henrik")));
        // Note that driver Ids are only valid for the day
        Set<String> followDriverIds = prefs.getStringSet("followDriverIds", new HashSet<String>(Collections.emptyList()));
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
        driverNamesDropDown.setText(String.join("\n", followDriverNames));
        driverNamesDropDown.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                return;
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String[] names = driverNamesDropDown.getText().toString().split("\\n");
                System.out.println(names);
                Set<String> followDriverNames = new HashSet<String>(Arrays.asList(names));
                handler.followDriverNames = followDriverNames;
                SharedPreferences.Editor prefsEdit = prefs.edit();
                String driverIdDate = formattedDate;
                prefsEdit.putString("followDriverNames", String.join("\n", followDriverNames));
                prefsEdit.putString("driverIdDate", formattedDate);
                prefsEdit.apply();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                return;
            }
        });

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
        mTextView = findViewById(R.id.text_view);
        tts.speak(initialMessage, TextToSpeech.QUEUE_ADD, null, null);
        handler = new MessageHandler(this.followDriverNames);
        AssetManager assetManager = getAssets();

//        System.out.println("testrun is: "+testRun);
        if (testRun.startsWith("true")){
            System.out.println("Doing test run with test data, no network connections created.");
            try {
                InputStream testDataInputStream = assetManager.open("testdata.txt");
                BufferedReader testDataReader = new BufferedReader(new InputStreamReader(testDataInputStream));
                       String line = null;
                       try {
                           while ((line = testDataReader.readLine()) != null) {
                               if(line.equals(""))
                                   continue;
//                               System.out.println("first line: " + line);

                               StringBuilder message = new StringBuilder();
                               message.append(line).append("\n");
                               while ((line = testDataReader.readLine()) != null && !line.equals("")) {
//                                   System.out.println("multiline: " + line);
                                   message.append(line).append("\n");
                               }
                               String englishMessage = handler.message(message.toString());
                               if(! (englishMessage.equals(""))){
                                   tts.speak(englishMessage, TextToSpeech.QUEUE_ADD, null, null);
                                   runOnUiThread(new Runnable() {
                                       @Override
                                       public void run() {
                                           mTextView.setText(englishMessage);
                                       }
                                   });
                               }
                           }
                       } catch(IOException ex){
                           ex.printStackTrace();
                       } finally{
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
            try {
                URI serverUri = new URI(websocketUrl);
                mWebSocket = new WebSocketManager(serverUri, new MyWebsocketListener() {
                    @Override
                    public void onMessageReceived(final String message) {
                        // Called when a message is received from the server
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
                    public void onConnected(){System.out.println("Websocket connected");}
                    public void onDisconnected(){System.out.println("Websocket disconnected");}
                    public void onError(Exception ex){System.err.println("websocket error: " + ex);}
                });
                mWebSocket.connect();
            } catch (URISyntaxException e) {
                System.out.println("Invalid WebSocket URI: " + e.getMessage());
            }
        }
    }
    @Override
    public void onPause(){
        super.onPause();
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close WebSocket connection
        if (mWebSocket != null) {
            mWebSocket.close();
        }
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
    }
}