import javax.websocket.*;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class GameClient {
    private String serverAddress;
    private Session userSession;
    private OnMessageReceivedListener messageListener;
    private OnPlayerJoinListener playerJoinListener;
    private static CountDownLatch latch;
    private OnQuestionReceivedListener questionListener;

    // Interface to handle received messages
    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    // Interface to handle player join events
    public interface OnPlayerJoinListener {
        void onPlayerJoin(String playerName);
    }

    // Interface to handle received questions
    public interface OnQuestionReceivedListener {
        void onQuestionReceived(String question, String[] answers, int correctIndex);
    }

    /**
     * Client constructor.
     * 
     * @param serverAddress The IP or domain of the server.
     */
    public GameClient(String serverAddress) {
        this.serverAddress = "ws://" + serverAddress + "/game"; // WebSocket URL
    }

    /**
     * Sets the listener for handling messages from the server.
     * 
     * @param listener The listener instance.
     */
    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.messageListener = listener;
    }

    /**
     * Sets the listener for handling player join events.
     * 
     * @param listener The listener instance.
     */
    public void setOnPlayerJoinListener(OnPlayerJoinListener listener) {
        this.playerJoinListener = listener;
    }

    /**
     * Sets the listener for handling incoming questions.
     * 
     * @param listener The listener instance.
     */
    public void setOnQuestionReceivedListener(OnQuestionReceivedListener listener) {
        this.questionListener = listener;
    }

    /**
     * Establishes a WebSocket connection to the server.
     * 
     * @return true if the connection attempt is initiated, false if there's an
     *         error.
     */
    public boolean connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = new URI(serverAddress);
            latch = new CountDownLatch(1); // Used to block the thread until connection is established
            container.connectToServer(new ClientEndpoint(), uri);
            latch.await(); // Wait until connected
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Sends message to server
    public void sendMessage(String message) {
        if (userSession != null && userSession.isOpen()) {
            try {
                userSession.getBasicRemote().sendText(message);
                // Log.i("GameClient", "Sent: " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Disconnects websocket connection
    public void closeConnection() {
        if (userSession != null) {
            try {
                userSession.close();
                // Log.i("GameClient", "Disconnected from WebSocket server.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Client endpoint to handle WebSocket events
    public class ClientEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            // Log.i("GameClient", "Connected to WebSocket server!");
            userSession = session;
            latch.countDown(); // Allow the thread to continue after connection is established
        }

        @OnMessage
        public void onMessage(String message) {
            // Log.i("GameClient", "Received: " + message);

            if (messageListener != null) {
                messageListener.onMessageReceived(message);
            }

            if (message.startsWith("Player")) { // Check if message is a player joining
                if (playerJoinListener != null) {
                    // Log.d("GameClient", "Notifying player join: " + message);
                    playerJoinListener.onPlayerJoin(message);
                }
            } else if (message.startsWith("question|")) { // Check if message is a question
                handleQuestionMessage(message);
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            // Log.i("GameClient", "Connection closed: " + closeReason.getReasonPhrase());
        }

        @OnError
        public void onError(Session session, Throwable throwable) {
            // Log.e("GameClient", "Error: " + throwable.getMessage());
            throwable.printStackTrace();
        }

        // Handle question messages from the server
        private void handleQuestionMessage(String message) {
            try {
                // Split the message by the pipe symbol
                String[] parts = message.split("\\|");

                // Ensure message format is correct
                if (parts.length == 4) {
                    String question = parts[1];
                    String[] answers = parts[2].split(",");
                    int correctIndex = Integer.parseInt(parts[3]);

                    // Notify the listener with the extracted question and answers
                    if (questionListener != null) {
                        questionListener.onQuestionReceived(question, answers, correctIndex);
                    }
                } else {
                    // Invalid question format
                    System.out.println("Invalid question format: " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
