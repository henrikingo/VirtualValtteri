package cc.openlife.virtualvaltteri;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import cc.openlife.virtualvaltteri.vmkarting.MessageHandler;
public class MainActivity extends AppCompatActivity {
    private WebSocketManager mWebSocket;
    private TextView mTextView;
    private MessageHandler handler;
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

        // Get WebSocket URL from properties file
        String websocketUrl = null;
        String testRun = "false";
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            websocketUrl = properties.getProperty("websocket.url");
            testRun = properties.getProperty("testrun");
        } catch (IOException e) {
            System.out.println("Failed to read properties file: " + e.getMessage());
        }

        handler = new MessageHandler();

//        System.out.println("testrun is: "+testRun);
        if (testRun.startsWith("true")){
            System.out.println("Doing test run with test data, no network connections created.");
            try {
                InputStream testDataInputStream = assetManager.open("testdata.txt");
                BufferedReader testDataReader = new BufferedReader(new InputStreamReader(testDataInputStream));
/*                Timer timer = new Timer();
                timer.schedule(new TimerTask(){
                   @Override
                   public void run() {
*/
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
                               runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       String englishMessage = handler.message(message.toString());
                                       mTextView.setText(englishMessage);
                                   }
                               });
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
/*                   }
               },
                100,
                1000);

 */
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
/*
        // Connect to WebSocket server
        if (websocketUrl != null) {
            try {
                URI serverUri = new URI(websocketUrl);
                mWebSocket = new WebSocketManager(serverUri, new MyWebsocketListener() {
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
                });
                mWebSocket.connect();
            } catch (URISyntaxException e) {
                System.out.println("Invalid WebSocket URI: " + e.getMessage());
            }
        }
*/
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close WebSocket connection
        if (mWebSocket != null) {
            mWebSocket.close();
        }
    }
}