//Package

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/game") //Directory where client will connect to
public class GameServer {
    private static final Set<Session> clients = new CopyOnWriteArraySet<>();
    private static final int MAX_PLAYERS = 6; //Amount of players allowed 
    private static boolean gameStarted = false;

    private static int clientID=1;
    private static final Map<Session, Integer> clientIDs = new HashMap<>(); //Map to associate ids with sessions

    //When a client connects
    @OnOpen
    public void onOpen(Session session) {
        if(gameStarted){
            try { //Closes connection if a player tries to join after game has started
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (clients.size()<MAX_PLAYERS) {
            clientIDs.put(session, clientID); //Saves id and session in map
            clientID++;

            clients.add(session);
            try {
                session.getBasicRemote().sendText("You are player " + clientIDs.get(session));
            } catch (IOException e) {
                e.printStackTrace();
            }
            broadcastMessage("Player joined. Total players: " + clients.size());
        } else {
            try { //Closes connection if a player tries to join after max players limit
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //When a client leaves
    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
        clientIDs.remove(session);
        broadcastMessage("A player left. Total players: " + clients.size());
    }

    //When a message is received
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Received: " + message);

        //If server recives start message and game is not started, start game
        if(message.equals("start") && !gameStarted) {
            gameStarted = true;
            startGame();
        }

        broadcastMessage(message);
    }

    //If an error occurs
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    //Broadcasts messages to all connected clients
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
        while(gameStarted){
            try{
                
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        broadcastMessage("Game starting!");
    }
}
