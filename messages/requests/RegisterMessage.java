package messages.requests; 

public class RegisterMessage{
    public String operation = "register"; 
    public String name; 
    public String psw; 

    public RegisterMessage(String name, String psw){
        this.name = name; 
        this.psw = psw; 
    }
}
