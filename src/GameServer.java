// Package

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/game") // Directory where client will connect to
public class GameServer {
    private static final Set<Session> clients = new CopyOnWriteArraySet<>();
    private static final int MAX_PLAYERS = 6; // Amount of players allowed
    private static boolean gameStarted = false;

    private static int clientID = 1;
    private static final Map<Session, Integer> clientIDs = new HashMap<>(); // Map to associate ids with sessions
    private static final Map<Session, Integer> playerScores = new HashMap<>(); // Map to store players' scores

    // When a client connects
    @OnOpen
    public void onOpen(Session session) {
        if (gameStarted) {
            try { // Closes connection if a player tries to join after game has started
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (clients.size() < MAX_PLAYERS) {
            clientIDs.put(session, clientID); // Saves id and session in map
            playerScores.put(session, 0); // Initialize score to 0
            clientID++;

            clients.add(session);
            try {
                session.getBasicRemote().sendText("You are player " + clientIDs.get(session));
            } catch (IOException e) {
                e.printStackTrace();
            }
            broadcastMessage("Player joined. Total players: " + clients.size());
        } else {
            try { // Closes connection if a player tries to join after max players limit
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // When a client leaves, remove all session data
    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
        clientIDs.remove(session);
        playerScores.remove(session);
        broadcastMessage("A player left. Total players: " + clients.size());
    }

    // When a message is received
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Received: " + message);

        if (message.equals("start") && !gameStarted) { // If server receives start message and game is not started,
                                                       // start
                                                       // game
            gameStarted = true;
            startGame();
        } else if (message.startsWith("question ")) { // When it receives a question request, sends one
            getQuestion(message, session);
        } else if (message.startsWith("answer ")) { // When a player submits an answer
            checkAnswer(message, session);
        }

        broadcastMessage(message);
    }

    // If an error occurs
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    // Broadcasts messages to all connected clients
    public static void broadcastMessage(String message) {
        for (Session client : clients) {
            try {
                client.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void startGame() {
        // Game start logic here
        while (gameStarted) {
            try {
                // Game continues running
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        broadcastMessage("Game starting!");
    }

    /**
     * Grabs a question and sends it to the client.
     * 
     * @param message Message sent to server starting with "question ".
     * @param session Client session.
     */
    private void getQuestion(String message, Session session) {
        String[] parts = message.split(" ");
        if (parts.length != 3) {
            System.out.println("Invalid question request format.");
            return;
        }

        String difficulty = parts[1];
        String category = parts[2];

        Map<String, Object> questionData = QuestionManager.getRandomQuestion(category, difficulty);
        if (questionData == null) {
            System.out.println("No questions found for " + category + " - " + difficulty);
            return;
        }

        // Cast the Object to a List<String> before using toArray
        List<String> answersList = (List<String>) questionData.get("answers");
        String[] answersArray = answersList.toArray(new String[0]);

        // Now you can join the elements
        String questionMessage = "question|" + questionData.get("question") + "|"
                + String.join(",", answersArray) + "|"
                + questionData.get("correctIndex");
        broadcastMessage(questionMessage);
    }

    /**
     * Checks if the answer is correct and updates the player's score.
     * 
     * @param message Message sent to server starting with "answer ".
     * @param session Client session.
     */
    private void checkAnswer(String message, Session session) {
        String[] parts = message.split(" ");
        if (parts.length != 2) {
            System.out.println("Invalid answer format.");
            return;
        }

        String answer = parts[1]; // The answer submitted by the player

        // Get the correct answer index (you can store it earlier when sending the
        // question)
        int correctIndex = Integer.parseInt(parts[1]);

        // Check if the answer is correct
        if (Integer.parseInt(answer) == correctIndex) {
            // Increase player's score
            playerScores.put(session, playerScores.get(session) + 1);
            broadcastMessage("Player " + clientIDs.get(session) + " answered correctly!");
        } else {
            broadcastMessage("Player " + clientIDs.get(session) + " answered incorrectly.");
        }

        // Broadcast updated scores
        broadcastScores();
    }

    /**
     * Broadcasts all player scores.
     */
    private void broadcastScores() {
        StringBuilder scoreMessage = new StringBuilder("Scores:");
        for (Map.Entry<Session, Integer> entry : playerScores.entrySet()) {
            scoreMessage.append(" Player " + clientIDs.get(entry.getKey()) + ": " + entry.getValue() + " |");
        }
        broadcastMessage(scoreMessage.toString());
    }
}
