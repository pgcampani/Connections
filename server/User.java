package server; 

public class User{
    public String username;
    public String password; 
    public int totalScore; 
    public int gamesPlayed; 
    public int gamesWon; 
    public int currentGameid; 
    public boolean  isLogged; 

    public User(String password){
        this.password = password; 
        this.totalScore = 0; 
        this.gamesPlayed = 0;
        this.gamesWon = 0; 
        this.currentGameid = -1;  // Non sta giocando
        this.isLogged = false; 
    }
}
