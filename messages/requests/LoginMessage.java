package messages.requests; 

public class LoginMessage{

    public String operation = "login"; 
    public String username;
    public String password; 
    public int udpPort;
    
    public LoginMessage(String username, String password, int udpPort){
        this.username = username; 
        this.password = password; 
        this.udpPort = udpPort; 
    }
}
