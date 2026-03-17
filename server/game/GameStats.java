package server.game; 

import java.util.List;

import messages.Group; 

public class GameStats{
    public int gameId; 
    public int totalPlayers; 
    public int finishedPlayers; 
    public int wonPlayers; 
    public double totalScore; 
    public boolean concluded;
    public List<Group> groups; 

    public GameStats(int gameId){
        this.gameId = gameId; 
        this.totalPlayers = 0; 
        this.finishedPlayers = 0; 
        this.wonPlayers = 0; 
        this.totalScore = 0.0;
        this.concluded = false;
    }

    public synchronized void addPlayer(){
        totalPlayers++; 
    }

    public synchronized void finalizePlayer(boolean won, int score, boolean finished){
        if(finished){
            finishedPlayers++; 
        }
        
        if(won){
            wonPlayers++; 
        }
        totalScore += score; 
    }

    public synchronized double getAverageScore(){
        if(totalPlayers == 0){
            return 0; 
        }
        return totalScore/totalPlayers; 
    }

    public synchronized int getActivePlayers(){
        return totalPlayers - finishedPlayers; 
    }
}
