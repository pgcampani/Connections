package messages.requests;

public class RequestLeaderboardMessage{
    public String operation = "requestLeaderboard";
    public String playerName;
    public int topPlayers; 

    public RequestLeaderboardMessage(String playerName, int topPlayers){
        this.playerName = playerName;
        this.topPlayers = topPlayers; 
    }
}
