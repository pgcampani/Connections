package messages.requests; 

public class RegisterMessage{
    public String operation = "register"; 
    public String name; 
    public String password; 

    public RegisterMessage(String name, String password){
        this.name = name; 
        this.password = password; 
    }
}
