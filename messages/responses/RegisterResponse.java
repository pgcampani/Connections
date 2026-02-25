package  messages.responses; 

public class RegisterResponse{
    public String status; 
    public String message; 
    
    public RegisterResponse(String status, String message){
        this.status = status; 
        this.message = message; 
    }
}
