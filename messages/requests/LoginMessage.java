package messages.requests; 

public class LoginMessage{

    public String operation = "login"; 
    public String username;
    public String password; 
    
    public LoginMessage(String username, String password){
        this.username = username; 
        this.password = password; 
    }
}
