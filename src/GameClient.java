

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.*;
import java.net.*;

public class GameClient {
    private static final String TAG = "GameClient"; // Tag for logging

    private static GameClient instance;
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private List<String> playerNames = new ArrayList<>(); // Store player names
    private boolean isHost;

    private OnMessageReceivedListener messageListener;
    private OnPlayerJoinListener playerJoinListener;
    private OnQuestionReceivedListener questionListener;
    private OnHostStatusListener hostStatusListener;


    private static final Scanner scanner = new Scanner(System.in); // Global scanner  (Remove in production)

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    // Interface to handle player join events
    public interface OnPlayerJoinListener {
        void onPlayerJoin(String playerName);
        void onPlayerListReceived(List<String> playerNames); // New method
    }

    // Interface to handle received questions
    public interface OnQuestionReceivedListener {
        void onQuestionReceived(String question, String[] answers, int correctIndex);
    }

    // Interface to handle host status
    public interface OnHostStatusListener {
        void onHostStatusReceived(boolean isHost);
    }

    public static GameClient getInstance(String serverAddress, int serverPort) {
        if (instance == null || !instance.isConnected()) {
            instance = new GameClient(serverAddress, serverPort);
            if (!instance.connect()) {
                instance = null; // Reset instance if connection fails
            }
        }
        return instance;
    }

    public static GameClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GameClient not initialized. Call getInstance(address, port) first.");
        }
        return instance;
    }


    private GameClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
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
        if (out != null && isConnected()) {
            out.println(message);
        } else {
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
        } finally {
            socket = null;
            instance = null;
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
                } else if(message.equals("Host")){
                    setHost(true);
                    if (hostStatusListener != null) {
                        hostStatusListener.onHostStatusReceived(true);
                    }
                } else if (message.startsWith("players|")) {
                    handlePlayerListMessage(message);
                } else if (message.startsWith("Player ")) { // Handle join/leave
                    handlePlayerUpdateMessage(message);
                } else if (message.startsWith("question|")) {
                    handleQuestionMessage(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
            closeConnection();
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
        } catch (NumberFormatException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePlayerListMessage(String message) {
        String playerListStr = message.substring("players|".length());
        String[] players = playerListStr.split(",");
        playerNames.clear(); // Clear existing list

        for (String player : players) {
            if (!player.isEmpty()) { // Check for empty strings
                playerNames.add(player);
            }
        }

        // Notify listeners (if any) that the player list has been updated
        if (playerJoinListener != null) {
            playerJoinListener.onPlayerListReceived(playerNames);
        }
    }

    private void handlePlayerUpdateMessage(String message) {
        String playerName = message;
        if (message.endsWith("joined")) {
            playerName = playerName.substring(7, playerName.length() - 7).trim(); // Extract name and trim
            playerNames.add(playerName);
        } else if (message.endsWith("left")) {
            playerName = playerName.substring(7, playerName.length() - 5).trim(); // Extract name and trim
            playerNames.remove(playerName);
        }

        // Notify listeners (if any) that the player list has been updated
        if (playerJoinListener != null) {
            playerJoinListener.onPlayerListReceived(playerNames);
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

    public void setOnHostStatusListener(OnHostStatusListener listener) {
        this.hostStatusListener = listener;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    // Testing server (Remove in production)
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