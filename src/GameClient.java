import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private OnMessageReceivedListener messageListener;
    private OnPlayerJoinListener playerJoinListener;
    private OnQuestionReceivedListener questionListener;

    private static final Scanner scanner = new Scanner(System.in); // Global scanner

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

    public GameClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Establishes a WebSocket connection to the server.
     * 
     * @return Returns true if connected, false if failed.
     */
    public boolean connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::listenForMessages).start(); // Start listening for server messages
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends message to server
     * 
     * @param message Message.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Closes server connection
     */
    public void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listens for messages.
     */
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Server: " + message);

                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }

                if (message.startsWith("Player")) {
                    if (playerJoinListener != null) {
                        playerJoinListener.onPlayerJoin(message);
                    }
                } else if (message.startsWith("question|")) {
                    handleQuestionMessage(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }

    /**
     * Handles questions received from the server.
     * 
     * @param message Question.
     */
    private void handleQuestionMessage(String message) {
        try {
            // Ensure message format is correct
            String[] parts = message.split("\\|");

            // Ensure message format is correct
            if (parts.length == 4) {
                String question = parts[1];
                String[] answers = parts[2].split(",");
                int correctIndex = Integer.parseInt(parts[3]);

                if (questionListener != null) {
                    questionListener.onQuestionReceived(question, answers, correctIndex);
                }
            } else {
                System.out.println("Invalid question format: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.messageListener = listener;
    }

    public void setOnPlayerJoinListener(OnPlayerJoinListener listener) {
        this.playerJoinListener = listener;
    }

    public void setOnQuestionReceivedListener(OnQuestionReceivedListener listener) {
        this.questionListener = listener;
    }

    // Testing server
    public static void main(String[] args) {
        System.out.print("Enter server IP: ");
        String serverIP = scanner.nextLine();

        System.out.print("Enter server port: ");
        int port = scanner.nextInt();
        scanner.nextLine();

        GameClient client = new GameClient(serverIP, port);

        if (client.connect()) {
            System.out.println("Connected to the game server!");

            new Thread(() -> {
                while (true) {
                    String message = scanner.nextLine();
                    if (message.equalsIgnoreCase("exit")) {
                        client.closeConnection();
                        System.out.println("Disconnected from server.");
                        break;
                    }
                    client.sendMessage(message);
                }
            }).start();
        } else {
            System.out.println("Failed to connect to server.");
        }
    }
}
