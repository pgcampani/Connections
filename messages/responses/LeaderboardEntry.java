package messages.responses; 

public class LeaderboardEntry {
    public int rank; 
    public String username; 
    public int score; 

    public LeaderboardEntry(int rank, String username, int score){
        this.rank = rank; 
        this.username = username; 
        this.score = score; 
    }
}
