package Server.messages.server;

import java.util.List;

import Game.Models.Player;
import Server.messages.Message;

public class GameOver extends Message{
    private static final long serialVersionUID = 102L;
    
    public List<Player> players;

    public GameOver(List<Player> players){
        this.players = players;
    }
}
