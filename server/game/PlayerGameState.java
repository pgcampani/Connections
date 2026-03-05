package server.game; 

import java.util.ArrayList;
import java.util.List;

import messages.Group; 

public class PlayerGameState{
    public int gameId; 
    public int errors; 
    public int score; 
    public boolean finished; 
    public List<Group> correctGroups; 

    public PlayerGameState(int gameId){
        this.gameId = gameId; 
        this.errors = 0; 
        this.score = 0; 
        this.finished = false; 
        this.correctGroups = new ArrayList<>(); 
    }

    public boolean hasWon(){
        return correctGroups.size() == 3; 
    }

    public boolean hasLost(){
        return errors == 4; 
    }
}
