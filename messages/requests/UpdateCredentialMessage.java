package messages.requests; 

public class UpdateCredentialMessage{
    public String operation = "updateCredentials";
    public String oldName; 
    public String oldPsw;
    public String newName;
    public String newPsw;

    public UpdateCredentialMessage(String oldName, String oldPsw, String newName, String newPsw){
        this.oldName = oldName;
        this.oldPsw = oldPsw; 
        this.newName = newName;
        this.newPsw = newPsw; 
    }
}
