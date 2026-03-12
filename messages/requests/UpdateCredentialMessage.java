package messages.requests; 

public class UpdateCredentialMessage{
    public String operation = "updateCredential";
    public String old_username; 
    public String old_psw;
    public String new_username;
    public String new_psw;

    public UpdateCredentialMessage(String old_username, String old_psw, String new_username, String new_psw){
        this.old_username = old_username;
        this.old_psw = old_psw; 
        this.new_username = new_username;
        this.new_psw = new_psw; 
    }
}
