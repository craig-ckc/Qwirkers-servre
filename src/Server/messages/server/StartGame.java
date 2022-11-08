package Server.messages.server;

import java.util.List;

import Game.Models.Player;
import Server.messages.Message;
 
public class StartGame  extends Message{
    private static final long serialVersionUID = 108L;

    public List<Player> players;
    public Player player;
    public int bag;

    public StartGame(List<Player> players, Player player, int bag) {
        this.players = players;
        this.player = player;
        this.bag = bag;
    }
}
