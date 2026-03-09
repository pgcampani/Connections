package server.game; 

import java.util.concurrent.ConcurrentHashMap;

public class GameManagerState{
    public int nextGameIndex;
    public ConcurrentHashMap<Integer, GameStats> gameStats; 
    
    public GameManagerState(int nextGameIndex, ConcurrentHashMap<Integer, GameStats> gameStats){
        this.nextGameIndex = nextGameIndex;
        this.gameStats = gameStats; 
    }
}
