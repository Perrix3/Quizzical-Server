import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 8089;
    private static final int MAX_PLAYERS = 6;

    private static boolean gameStarted = false;
    private static int clientID = 1;

    private static final Set<ClientHandler> clients = new HashSet<>();
    private static final Map<ClientHandler, Integer> playerScores = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                if (gameStarted || clients.size() >= MAX_PLAYERS) {
                    clientSocket.close(); // Reject new players if game started or full
                    continue;
                }

                ClientHandler clientHandler = new ClientHandler(clientSocket, clientID++);
                clients.add(clientHandler);
                playerScores.put(clientHandler, 0);
                new Thread(clientHandler).start();

                broadcastMessage("Player joined. Total players: " + clients.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Sends a message to all players */
    private static void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /** Handles client communication */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerID;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket, int playerID) {
            this.socket = socket;
            this.playerID = playerID;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                sendMessage("You are player " + playerID);

                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Player " + playerID + " disconnected.");
            } finally {
                closeConnection();
            }
        }

        /** Processes messages from clients */
        private void handleMessage(String message) {
            System.out.println("Received from Player " + playerID + ": " + message);

            if (message.equals("start") && !gameStarted) {
                gameStarted = true;
                broadcastMessage("Game starting!");
            } else if (message.startsWith("roll")) {
                int roll = (int) (Math.random() * 6) + 1;
                broadcastMessage("Player " + playerID + " rolled " + roll);
            } else if (message.startsWith("answer ")) {
                checkAnswer(message);
            } else {
                broadcastMessage("Player " + playerID + ": " + message);
            }
        }

        /** Checks player answers and updates score */
        private void checkAnswer(String message) {
            String[] parts = message.split(" ");
            if (parts.length != 2)
                return;

            int correctIndex = Integer.parseInt(parts[1]); // Simulated correct answer
            int playerAnswer = Integer.parseInt(parts[1]);

            if (playerAnswer == correctIndex) {
                playerScores.put(this, playerScores.get(this) + 1);
                broadcastMessage("Player " + playerID + " answered correctly!");
            } else {
                broadcastMessage("Player " + playerID + " answered incorrectly.");
            }
            broadcastScores();
        }

        /** Broadcasts updated scores */
        private void broadcastScores() {
            StringBuilder scoreMessage = new StringBuilder("Scores: ");
            for (ClientHandler client : clients) {
                scoreMessage.append("Player ").append(client.playerID).append(": ")
                        .append(playerScores.get(client)).append(" | ");
            }
            broadcastMessage(scoreMessage.toString());
        }

        /** Sends a message to this player */
        private void sendMessage(String message) {
            out.println(message);
        }

        /** Closes connection and removes player */
        private void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clients.remove(this);
            playerScores.remove(this);
            broadcastMessage("Player " + playerID + " left. Total players: " + clients.size());
        }
    }
}
