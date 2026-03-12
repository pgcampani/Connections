package messages.responses; 

public class GameStatsResponse{
    public String status; 
    public String message; 
    public long timeRemaining; 
    public int playerInGame; 
    public int playerFinished; 
    public int playerWinners; 
    public int playerCount;
    public double avgPointsGame; 

    public GameStatsResponse(){}

    public static GameStatsResponse inProgress(long timeRemaining, int playerInGame, int playerFinished, int playerWinners){
        GameStatsResponse gameStats = new GameStatsResponse(); 

        gameStats.status = "IN_PROGRESS";
        gameStats.timeRemaining = timeRemaining; 
        gameStats.playerInGame = playerInGame;
        gameStats.playerFinished = playerFinished; 
        gameStats.playerWinners = playerWinners; 

        return gameStats; 
    }

    public static GameStatsResponse concluded(int playerCount, int playerFinished, int playerWinners, double avgPointsGame){
        GameStatsResponse gameStats = new GameStatsResponse(); 

        gameStats.status = "CONCLUDED"; 
        gameStats.playerCount = playerCount; 
        gameStats.playerFinished = playerFinished; 
        gameStats.playerWinners = playerWinners; 
        gameStats.avgPointsGame = avgPointsGame; 

        return gameStats; 
    }


    public static GameStatsResponse error(String status, String message){
        GameStatsResponse gameStats = new GameStatsResponse(); 
        gameStats.status = status;
        gameStats.message = message; 

        return gameStats; 
    }
}
