package messages.responses;

import java.util.List; 
import messages.Group; 

public class GameInfoResponse{
    public String status; 
    public String message; 
    public List<String> words; 
    public List<Group> correctGroups; 
    public int errors; 
    public long timeRemaining; 
    public int score; 

    public GameInfoResponse(List<String> words, List<Group> corretctGroups, int errors, long timeRemaining, int score){
        this.status = "OK";
        this.words = words; 
        this.correctGroups = corretctGroups; 
        this.errors = errors;
        this.timeRemaining = timeRemaining; 
        this.score = score; 
    }

    public GameInfoResponse(String status, String message){
        // Se nessuna partita in corso
        this.status = status;
        this.message = message; 
    }
}
