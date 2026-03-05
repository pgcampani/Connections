package  messages.requests; 

import java.util.List;

public class SubmitProposalMessage {
    public String operation = "submit_proposal"; 
    public List<String> words; 

    public SubmitProposalMessage(List<String> words){
        this.words = words; 
    }
}
