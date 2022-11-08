package Server.messages.server;

import java.util.List;

import Game.Models.Tile;
import Server.messages.Message;
 
public class Traded  extends Message{
    private static final long serialVersionUID = 109L;

    public List<Tile> tiles;

    public Traded(List<Tile> tiles) {
        this.tiles = tiles;
    }
}
