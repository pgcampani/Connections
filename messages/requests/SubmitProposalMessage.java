package  messages.requests; 

import java.util.List;

public class SubmitProposalMessage {
    public String operation = "submitProposal"; 
    public List<String> words; 

    public SubmitProposalMessage(List<String> words){
        this.words = words; 
    }
}
