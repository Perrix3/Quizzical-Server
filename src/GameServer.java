import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 8089;
    private static final int MAX_PLAYERS = 6;

    private static boolean gameStarted = false;
    private static int clientID = 1;
    private static String difficulty;
    private static int currentPlayerIndex = 0;
    private static boolean gameOver = false;

    private static final Set<ClientHandler> clients = new HashSet<>();
    private static final Map<ClientHandler, Integer> playerScores = new HashMap<>();
    private static List<ClientHandler> playerList = new ArrayList<>(); // Maintain player order

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
            playerList.add(this);
        }

        @Override
        public void run() {
            try {
                // Initialize input and output streams only once here to avoid
                // NullPointerException
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true); // true for auto-flush

                sendMessage("You are player " + playerID);
                System.out.println("Player " + playerID + " has joined.");

                // Broadcast the updated player list to *all* clients when a new player joins
                sendPlayerList(this, true);
                System.out.println("Sent player list to Player " + playerID);

                sendMessage("id|" + playerID);
                System.out.println("Sent id to player " + playerID);

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

            if (message.startsWith("start") && gameStarted == false) {
                String[] parts = message.split("\\|");
                difficulty = parts[1];
                gameStarted = true;
                broadcastMessage("Start");
                System.out.println("Game started with difficulty " + difficulty);
                broadcastMessage("turn|" + playerList.get(currentPlayerIndex).playerID); // Then send the turn message
                System.out.println("First turn goes to Player "+playerID);
            } else if (message.startsWith("roll")) {
                int roll = (int) (Math.random() * 6) + 1;
                broadcastMessage("Player " + playerID + " rolled " + roll);
            } else if (message.startsWith("answer ")) {
                checkAnswer(message);
            } else if (message.equals("getPlayers")) {
                sendPlayerList(this, true);
            } else if (message.startsWith("question|")) {
                handleQuestionRequest(message);
            } else if (message.equals("endTurn")) {
                if (isTurn(this)) {
                    endTurn();
                }
            } else if (message.equals("win")) {
                handleWin(this);
            } else {
                broadcastMessage("Player " + playerID + ": " + message);
            }
        }

        /**
         * Check if it's x client's turn.
         * 
         * @param client Client to check turn.
         * @return True if it's current client's turn, false if not.
         */
        private boolean isTurn(ClientHandler client) {
            return playerList.indexOf(client) == currentPlayerIndex;
        }

        /**
         * Ends current client's turn and starts next turn.
         */
        private void endTurn() {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size(); // Cycle through players
            broadcastMessage("turn|" + playerList.get(currentPlayerIndex).playerID); // Signal next turn
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
            if (out != null) {
                out.println(message);
            } else {
                System.out.println("Error: Output stream is null for player " + playerID);
            }
        }

        private void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clients.remove(this);
            playerScores.remove(this);

            // Broadcast the updated player list to all clients when a player leaves
            sendPlayerList(this, true);
        }

        /**
         * Sends player list to a certain client if "sync" is false, and to all clients
         * if true.
         * 
         * @param client Client who made the call.
         * @param sync   True if want to broadcast message to all clients, false if not.
         */
        private void sendPlayerList(ClientHandler client, Boolean sync) {
            StringBuilder playerListMessage = new StringBuilder("players|");
            for (ClientHandler otherClient : clients) {
                playerListMessage.append("Player ").append(otherClient.playerID).append(",");
            }
            String message = playerListMessage.toString();
            if (message.endsWith(",")) {
                message = message.substring(0, message.length() - 1);
            }
            if (sync) {
                broadcastMessage(message);
            } else {
                client.sendMessage(message);
            }
        }

        private void handleWin(ClientHandler winner) {
            if (!gameOver) {
                gameOver = true;
                broadcastMessage("Player " + winner.playerID + " has won.");
                broadcastMessage("Game Over!");

                for (ClientHandler client : new HashSet<>(clients)) {
                    client.closeConnection();
                }
                clients.clear();
                playerScores.clear();
                playerList.clear();
                gameStarted = false;
                gameOver = false;
                clientID = 1;
                currentPlayerIndex = 0;

            }
        }

        private void handleQuestionRequest(String message) {
            String[] parts = message.split("\\|");
            String category = parts[1];
            String lang = parts[2];
            Map<String, Object> questionData = QuestionManager.getRandomQuestion(category, difficulty, lang); // Use
                                                                                                              // stored
                                                                                                              // difficulty

            if (questionData != null) {
                StringBuilder questionMessage = new StringBuilder("question|");
                questionMessage.append(questionData.get("question")).append("|");

                @SuppressWarnings("unchecked")
                List<String> answers = (List<String>) questionData.get("answers");

                for (int i = 0; i < answers.size(); i++) {
                    questionMessage.append(answers.get(i));
                    if (i < answers.size() - 1) {
                        questionMessage.append(",");
                    }
                }
                questionMessage.append("|").append(questionData.get("correctIndex"));

                sendMessage(questionMessage.toString()); // Send the formatted message
            } else {
                sendMessage("No questions found for that category, difficulty, and language.");
            }
        }

    }
}
