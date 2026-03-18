package messages.requests; 

public class LoginMessage{

    public String operation = "login"; 
    public String username;
    public String psw; 
    public int udpPort;
    
    public LoginMessage(String username, String psw, int udpPort){
        this.username = username; 
        this.psw = psw; 
        this.udpPort = udpPort; 
    }
}
