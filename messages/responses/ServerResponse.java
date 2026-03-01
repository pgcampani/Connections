package  messages.responses; 

public class ServerResponse{
    public String status; 
    public String message; 
    
    public ServerResponse(String status, String message){
        this.status = status; 
        this.message = message; 
    }
}
