package Server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import Game.Models.Player;
import Game.Models.ServerGame;
import Server.messages.Message;
import Server.messages.client.FinishPlay;
import Server.messages.client.Join;
import Server.messages.client.PlayMove;
import Server.messages.client.Leave;
import Server.messages.client.Trade;
import Server.messages.server.Confirmation;
import Server.messages.server.FinishedPlay;
import Server.messages.server.GameOver;
import Server.messages.server.Joined;
import Server.messages.server.Left;
import Server.messages.server.PlayedMove;
import Server.messages.server.Traded;

public class Games {
    private static final ReentrantLock lock = new ReentrantLock();

    public static final Map<String, ServerGame> games = new HashMap<>();

    public static void addGame(String gameSession) {
        lock.lock();

        games.put(gameSession, new ServerGame());

        lock.unlock();
    }

    public static void join(String session, Client client) {
        lock.lock();

        games.get(session).addClient(client);

        client.send(new Confirmation(session));

        lock.unlock();
    }

    public static void leave(Client client) {
        ServerGame game = games.get(client.session);

        boolean isCurrent = client == game.currentPlayer();

        lock.lock();

        if (isCurrent) game.nextPlayer();

        Player player = game.removeClient(client);
        game.arrangePlayers();

        lock.unlock();

        send(client.session, new Left(player.getName(), game.players()));

        send(client.session, new FinishedPlay(game.players(), game.bagSize(), game.player()));
    }

    public static void send(String session, Message message) {
        lock.lock();

        games.get(session).clients().forEach(client -> {
            client.send(message);
        });

        lock.unlock();
    }

    public static void onMessageReceived(Client client, Message message) {

        if (message instanceof Join) {
            Join msg = (Join) message;

            lock.lock();

            games.get(msg.session).registerPlayer(client, msg.player);
            client.name = msg.player.getName();

            lock.unlock();

            send(msg.session, new Joined(msg.player.getName(), games.get(msg.session).players()));

            System.out.println(client.ClientID + " : " + client.name + " has joined game " + client.session);
        }

        else if (message instanceof PlayMove) {
            PlayMove msg = (PlayMove) message;

            send(msg.session, new PlayedMove(msg.position, msg.tile));
        }

        else if (message instanceof Trade) {
            Trade msg = (Trade) message;

            send(msg.session, new Traded(client.name, msg.tiles));
        }

        else if (message instanceof FinishPlay) {
            FinishPlay msg = (FinishPlay) message;

            ServerGame game = games.get(msg.session);

            if(game.done()){
                send(msg.session, new GameOver(game.players()));
                return;
            }

            send(msg.session, new FinishedPlay(game.players(), game.bagSize(), game.player()));
        }

        else if (message instanceof Leave) {
            Leave msg = (Leave) message;

            send(msg.session, new Left(msg.name, games.get(msg.session).players()));
            System.out.println(client.ClientID + " : " + client.name + " has left game " + client.session);
        }

    }

}
