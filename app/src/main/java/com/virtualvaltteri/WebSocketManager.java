package com.virtualvaltteri;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketManager {
    private static MyWebSocketClient mWebSocketClient;
    private URI serverUri;
    private MainActivity activity;
    private boolean isRestarting = false;

    public WebSocketManager(String websocketUrl, MainActivity activity)
            throws URISyntaxException {
        this.serverUri = new URI(websocketUrl);
        this.activity = activity;
    }
    public WebSocketClient connect() {
        if (mWebSocketClient==null || mWebSocketClient.isClosed() || mWebSocketClient.isClosing()){
            if(mWebSocketClient!=null)
                mWebSocketClient.isClosedForever=true;
            mWebSocketClient = null;
        }
        if (mWebSocketClient==null || mWebSocketClient.isClosedForever){
            mWebSocketClient = new MyWebSocketClient(serverUri, activity);
            mWebSocketClient.connect();
        }
        return mWebSocketClient;
    }

    public class MyWebSocketClient extends  WebSocketClient {
        MainActivity activity;
        URI serverUri;
        public boolean isClosedForever = false;
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
            if(isClosedForever) return;

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
                    if (mWebSocketClient != null){
                        mWebSocketClient.isClosedForever = true;
                    }

                    mWebSocketClient = new MyWebSocketClient(serverUri, activity);
                    mWebSocketClient.connect();
                    isRestarting = false;
                }
            };
            t.schedule(task, 987);
        }
        public void onError(Exception ex){
            System.err.println("websocket error: " + ex);
            ex.printStackTrace();
            if(isRestarting){
                System.err.println("Already scheduled to restart. Not doing anything in this thread.");
                return;
            }

            isRestarting = true;
            System.err.println("Scheduling a re-connect in a minute...");
            Timer t = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mWebSocketClient.isClosedForever = true;
                    mWebSocketClient.close();

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