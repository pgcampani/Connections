package server.game; 

import java.util.List;

import messages.Group; 

public class GameStats{
    public int gameId; 
    public int totalPlayers; 
    public int finishedPlayers; 
    public int wonPlayers; 
    public double totalScore; 
    public boolean conculded;
    public List<Group> groups; 

    public GameStats(int gameId){
        this.gameId = gameId; 
        this.totalPlayers = 0; 
        this.finishedPlayers = 0; 
        this.wonPlayers = 0; 
        this.totalScore = 0.0;
        this.conculded = false;
    }

    public void addPlayer(){
        totalPlayers++; 
    }

    public void finalizePlayer(boolean won, int score){
        finishedPlayers++; 
        if(won){
            wonPlayers++; 
        }
        totalScore += score; 
    }

    public double getAverageScore(){
        if(totalPlayers == 0){
            return 0; 
        }
        return totalScore/totalPlayers; 
    }

    public int getActivePlayers(){
        return totalPlayers - finishedPlayers; 
    }
}
