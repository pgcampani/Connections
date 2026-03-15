package messages.responses;

import java.util.List; 

public class GameEndNotification{
    public String type = "GAME_END"; 
    public GameInfoResponse gameInfo;
    public GameStatsResponse gameStats;
    public List<LeaderboardEntry> ranking; 
}
