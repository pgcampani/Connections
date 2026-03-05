package server.game; 

import java.util.List;

import messages.Group; 

public class Game {
    public int gameId; 
    public List<Group> groups; 
    public transient long startTime; 
    public transient long endTime; 
}
