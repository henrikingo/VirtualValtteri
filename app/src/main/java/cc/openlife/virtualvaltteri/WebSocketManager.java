package cc.openlife.virtualvaltteri;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class WebSocketManager {
    private WebSocketClient mWebSocketClient;

    public WebSocketManager(URI serverUri, final MyWebsocketListener listener) {
        mWebSocketClient = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                listener.onConnected();
            }

            @Override
            public void onMessage(String message) {
                listener.onMessageReceived(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                listener.onDisconnected();
            }

            @Override
            public void onError(Exception ex) {
                listener.onError(ex);
            }
        };
    }

    public void connect() {
        mWebSocketClient.connect();
    }

    public void close() {
        mWebSocketClient.close();
    }
}