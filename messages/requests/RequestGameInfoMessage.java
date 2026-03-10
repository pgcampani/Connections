package messages.requests; 

public class RequestGameInfoMessage{
    public String operation = "request_game_info"; 
    public int gameId; 

    public RequestGameInfoMessage(int gameId){
        this.gameId = gameId; 
    }
}
