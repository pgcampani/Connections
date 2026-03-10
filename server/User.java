package server;

import java.util.HashMap; 
import java.util.Map;
 
import server.game.PlayerGameState; 

public class User{
    public String username;
    public String password; 
    public int totalScore; 
    public int gamesPlayed; 
    public int gamesWon; 
    public PlayerGameState currentGameState; 
    public Map<Integer, PlayerGameState> pastGames = new HashMap<>(); 
    public transient int currentGameid; 
    public transient boolean isLogged; 

    public User(String password){
        this.password = password; 
        this.totalScore = 0; 
        this.gamesPlayed = 0;
        this.gamesWon = 0; 
        this.currentGameid = -1;  // Non sta giocando
        this.isLogged = false; 
        this.currentGameState = null; 
    }
}