package messages.responses;

import java.util.List; 

import messages.Group; 

public class GameInfoResponse{
    public String status; 
    public String message; 
    public List<String> remainingWords;
    public List<Group> correctGroups; 
    public int errors; 
    public long timeRemaining; 
    public int score; 
    public List<Group> groups; 
    public int correctCount; 

    public GameInfoResponse(){}

    public static GameInfoResponse inProgress(long timeRemaining, List<Group> correctGroups, List<String> remainingWords, int errors, int score){
        GameInfoResponse r = new GameInfoResponse(); 
        r.status = "IN_PROGRESS";
        r.timeRemaining = timeRemaining; 
        r.correctGroups = correctGroups;
        r.remainingWords = remainingWords; 
        r.errors = errors; 
        r.score = score; 
        return r; 
    }

    public static GameInfoResponse concluded(List<Group> groups, int correctCount, int errors, int score){
        GameInfoResponse r = new GameInfoResponse();
        r.status = "CONCLUDED";
        r.groups = groups; 
        r.correctCount = correctCount; 
        r.errors = errors; 
        r.score = score; 
        return r; 
    }

    public static GameInfoResponse error(String status, String message){
        GameInfoResponse r = new GameInfoResponse(); 
        r.status = status;
        r.message = message; 
        return r; 
    }
}
