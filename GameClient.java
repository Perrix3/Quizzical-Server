
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class GameClient {
    private String serverAddress;
    private WebSocketClient webSocketClient;
    private OnMessageReceivedListener messageListener;
    private OnPlayerJoinListener playerJoinListener;

    //Interface to handle received messages
    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    //Interface to handle player join events
    public interface OnPlayerJoinListener {
        void onPlayerJoin(String playerName);
    }

    /**
     * Constructor that initializes the WebSocket URL using the server address.
     * @param serverAddress The IP or domain of the server.
     */
    public GameClient(String serverAddress) { //Needs server address
        this.serverAddress = "ws://" + serverAddress + "/game"; // WebSocket URL
    }

    /**
     * Sets the listener for handling incoming messages from the server.
     * @param listener The listener instance.
     */
    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.messageListener = listener;
    }

    /**
     * Sets the listener for handling player join events.
     * @param listener The listener instance.
     */
    public void setOnPlayerJoinListener(OnPlayerJoinListener listener) {
        this.playerJoinListener = listener;
    }

    /**
     * Establishes a WebSocket connection to the server.
     * @return true if the connection attempt is initiated, false if there's an error.
     */
    public boolean connect() {
        try {
            webSocketClient = new WebSocketClient(new URI(serverAddress)) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.i("GameClient", "Connected to WebSocket server");
                }

                @Override
                public void onMessage(String message) {
                    Log.i("GameClient", "Received: " + message);

                    if (messageListener != null) {//Tells message listener there is a message
                        messageListener.onMessageReceived(message);
                    }

                    if (message.startsWith("Player")) { //Check if message is a player joining
                        if (playerJoinListener != null) {
                            Log.d("GameClient", "Notifying player join: " + message);
                            playerJoinListener.onPlayerJoin(message);
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.i("GameClient", "Disconnected: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e("GameClient", "Error: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
            return true;
        } catch (URISyntaxException e) {
            Log.e("GameClient", "Invalid WebSocket URL: " + e.getMessage());
            return false;
        }
    }

    //Sends message to server
    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
            Log.i("GameClient", "Sent: " + message);
        }
    }

    //Disconnects websocket connection
    public void closeConnection() {
        if (webSocketClient != null) {
            webSocketClient.close();
            Log.i("GameClient", "Disconnected from WebSocket server.");
        }
    }
}
