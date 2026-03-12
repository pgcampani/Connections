package messages.requests; 

public class RequestGameInfoMessage{
    public String operation = "requestGameInfo"; 
    public int gameId; 

    public RequestGameInfoMessage(int gameId){
        this.gameId = gameId; 
    }
}
