package cc.openlife.virtualvaltteri;

public interface MyWebsocketListener {
        public void onConnected();
        public void onMessageReceived(String message);
        public void onDisconnected();
        public void onError(Exception ex);
}
