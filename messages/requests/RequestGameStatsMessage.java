package messages.requests; 

public class RequestGameStatsMessage{
    public String operation = "requestGameStats";
    public int gameId; 

    public RequestGameStatsMessage(int gameId){
        this.gameId = gameId; 
    }
}
