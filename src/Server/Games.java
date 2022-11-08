package Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import Game.Models.Game;
import Game.Models.Move;
import Game.Models.Tile;
import Server.messages.Message;
import Server.messages.client.Clear;
import Server.messages.client.Finish;
import Server.messages.client.GetValidMoves;
import Server.messages.client.Join;
import Server.messages.client.Leave;
import Server.messages.client.Pass;
import Server.messages.client.Play;
import Server.messages.client.Trade;
import Server.messages.client.Undo;
import Server.messages.server.Confirmation;
import Server.messages.server.Finished;
import Server.messages.server.InvalidMove;
import Server.messages.server.Joined;
import Server.messages.server.Left;
import Server.messages.server.Played;
import Server.messages.server.Refill;
import Server.messages.server.Undone;
import Server.messages.server.Update;
import Server.messages.server.ValidMoves;

public class Games {
    private static final ReentrantLock lock = new ReentrantLock();

    public static final Map<String, Game> games = new HashMap<>();
    public static final Map<String, Set<Client>> clients = new HashMap<>();

    public static void addGame(String gameSession) {
        lock.lock();

        games.put(gameSession, new Game());
        clients.put(gameSession, new HashSet<>());

        lock.unlock();
    }

    public static void join(String session, Client client) {
        lock.lock();

        clients.get(session).add(client);

        client.send(new Confirmation(session));

        lock.unlock();
    }

    public static void leave(Client client) {
        lock.lock();
        
        Game game = games.get(client.session);
        
        clients.get(client.session).remove(client);
        
        for(Move move : game.removePlayer(client.player)){
            send(client.session, new Undone(move.getPosition()));
        }

        client.session = "";

        send(client.session, new Left(client.player.name(), game.players(), game.currentPlayer(), game.bagCount()));

        lock.unlock();
    }

    public static void send(String session, Message message) {
        lock.lock();

        clients.get(session).forEach(client -> {
            client.send(message);
        });

        lock.unlock();
    }

    public static void onMessageReceived(Client client, Message message) {
        Game game = games.get(client.session);

        // Done
        if (message instanceof Clear) {
            List<Tile> refill = new ArrayList<>();

            for(Move move : game.clear()){
                refill.add(move.getTile());
                send(client.session, new Undone(move.getPosition()));
            }

            client.send(new Refill(refill));
        }

        // Done
        else if (message instanceof Finish) {
            game.done();

            send(client.session, new Finished(game.players(), game.currentPlayer(), game.bagCount()));
        }

        // Done
        else if (message instanceof GetValidMoves) {
            GetValidMoves msg = (GetValidMoves) message;

            client.send(new ValidMoves(game.validMoves(msg.tile)));
        }

        // Done
        else if (message instanceof Join) {
            Join msg = (Join) message;

            // attach player object to the client object
            client.player = msg.player;

            // add player to the appropriate game instance
            game.addPlayer(msg.player);

            // notify other clients of the same game that player has joined and updated player list
            send(client.session, new Joined(msg.player.name(), game.players()));
        }

        // Done
        else if (message instanceof Leave) {
            leave(client);
        }

        // Done
        else if (message instanceof Pass) {
            List<Tile> refill = new ArrayList<>();

            for(Move move : game.clear()){
                refill.add(move.getTile());
                send(client.session, new Undone(move.getPosition()));
            }

            client.send(new Refill(refill));
            
            game.nextPlayer();

            send(client.session, new Update(game.players(), game.currentPlayer(), game.bagCount()));
        }

        // Done
        else if (message instanceof Play) {
            Play msg = (Play) message;

            if(game.play(msg.tile, msg.position)){
                send(client.session, new Played(msg.position, msg.tile));
            }else{
                client.send(new InvalidMove(msg.tile));
            }

        }

        // Done
        else if (message instanceof Trade) {
            Trade msg = (Trade) message;

            client.send(new Trade(game.trade(msg.tiles)));
        }

        // Done
        else if (message instanceof Undo) {
            send(client.session, new Undone(game.undo().getPosition()));
        }
        
    }
}
