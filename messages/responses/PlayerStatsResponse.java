package messages.responses; 

public class PlayerStatsResponse{
    public String status;
    public String message; 
    public int puzzlesCompleted;
    public double winRate;
    public double lossRate; 
    public int currentStreak; 
    public int maxStreak; 
    public int perfectPuzzles; 
    public int[] mistakeHistogram; 
}
