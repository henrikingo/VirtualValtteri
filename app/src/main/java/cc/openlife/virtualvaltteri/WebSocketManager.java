package cc.openlife.virtualvaltteri;

import android.speech.tts.TextToSpeech;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketManager {
    private WebSocketClient mWebSocketClient;
    private URI serverUri;
    private MainActivity activity;
    private boolean isRestarting = false;

    public WebSocketManager(String websocketUrl, MainActivity activity)
            throws URISyntaxException {
        this.serverUri = new URI(websocketUrl);
        this.activity = activity;
    }
    public WebSocketClient connect() {
        mWebSocketClient = new MyWebSocketClient(serverUri, activity);
        mWebSocketClient.connect();
        return mWebSocketClient;
    }

    public class MyWebSocketClient extends  WebSocketClient {
        MainActivity activity;
        URI serverUri;
        MyWebSocketClient(URI serverUri, MainActivity activity) {
            super(serverUri);
            this.activity = activity;
            this.serverUri = serverUri;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("Websocket connected");
        }
        public void onMessage(final String message) {
            // Called when a message is received from the server
            activity.processMesssage(message);
        }
        public void  onClose(int code, String reason, boolean remote){
            System.out.println("Websocket disconnected - scheduling a re-connect in a sec...");
            if(isRestarting){
                System.err.println("Already scheduled to restart. Not doing anything in this thread.");
                return;
            }
            isRestarting = true;
            Timer t = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mWebSocketClient = new MyWebSocketClient(serverUri, activity);
                    mWebSocketClient.connect();
                    isRestarting = false;
                }
            };
            t.schedule(task, 987);
        }
        public void onError(Exception ex){
            System.err.println("websocket error: " + ex);
            if(isRestarting){
                System.err.println("Already scheduled to restart. Not doing anything in this thread.");
                return;
            }

            System.err.println("Scheduling a re-connect in a minute...");
            Timer t = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mWebSocketClient = new MyWebSocketClient(serverUri, activity);
                    mWebSocketClient.connect();
                    isRestarting = false;
                }
            };
            t.schedule(task, 6000);
        }
    }

    public void close() {
        mWebSocketClient.close();
        mWebSocketClient = null;
    }

}