package Server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import Game.Models.ServerGame;
import Server.messages.server.StartGame;

public class Server {
    public static void main(String[] args) throws Exception {
        new Server();
    }

    private ServerSocket server;
    private int ClientID = 0;

    int count = 1;
    String session = "game-" + count;
    long start = 0;

    public Server() throws Exception {
        server = new ServerSocket(5050);
        System.out.printf("Chat server started on: %s:5050\n", InetAddress.getLocalHost().getHostAddress());

        Games.addGame(session);

        new GamesManager();

        while (true) {
            Socket connection = server.accept();
            System.out.printf("Connection request received: %s\n", connection.getInetAddress().getHostAddress());

            ClientID++;

            Client client = new Client(connection, ClientID, session);
            client.setReceiver(message -> Games.onMessageReceived(client, message));

            Games.join(session, client);
        }
    }

    private class GamesManager extends Thread {
        private boolean countDown = false;

        public GamesManager() {
            start();
        }

        @Override
        public void run() {
            while(!isInterrupted()){
                if (Games.games.get(session).size() < 2)
                    continue;
                
                if(!countDown)
                    start = System.currentTimeMillis();
    
                Float duration = ((System.currentTimeMillis() - start) / 1000f);
    
                if (Games.games.get(session).size() > 4 || duration > 30) {

                    ServerGame game = Games.games.get(session);

                    Games.send(session, new StartGame(game.players(), game.bagSize(), game.start()));

                    count++;
                    session = "game-" + count;
                    start = 0;
                    
                    Games.addGame(session);

                    countDown = false;
                }
            }
        }

    }
}
