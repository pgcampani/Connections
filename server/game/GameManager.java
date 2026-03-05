package server.game; 

import messages.responses.*;
import messages.Group;  
import server.User; 
import server.UserManager;

import java.io.IOException;
import java.util.Collections; 
import java.util.List;
import java.util.ArrayList; 
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.Executors; 
import java.util.concurrent.ScheduledExecutorService; 
import java.util.concurrent.TimeUnit; 

public class GameManager{
    private final UserManager userManager; 
    private final GameLoader gameLoader; 
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); 
    private final int gameDuration; 
    private Game currentGame; 
    private List<String> shuffledWords; 

    public GameManager(UserManager userManager, String gameFile, int gameDuration) throws IOException{
        this.userManager = userManager; 
        this.gameLoader = new GameLoader(gameFile);
        this.gameDuration = gameDuration; 
    }

    public void start(){
        scheduler.scheduleAtFixedRate(this::startNewGame, 0, gameDuration, TimeUnit.SECONDS);
    }

    private synchronized void startNewGame(){
        try {
            Game game = gameLoader.loadNext();
            if(game == null){
                System.out.println("Nessuna partita disponibile");
                scheduler.shutdown();
                return;
            }

            List<String> words = new ArrayList<>();
            for(Group group : game.groups){
                words.addAll(group.words);
            }
            
            //Mescola le parole
            Collections.shuffle(words);

            game.startTime = System.currentTimeMillis();
            game.endTime = game.startTime + (gameDuration * 1000L);

            currentGame = game; 
            shuffledWords = words;

            System.out.println("Nuova partita avviata " + game.gameId); 
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public void stop(){
        scheduler.shutdown();

        try {
            gameLoader.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public synchronized Game getCurrentGame(){
        return currentGame;
    }

    public synchronized List<String> getShuffledWords(){
        return shuffledWords; 
    }

    public synchronized GameInfoResponse getGameInfo(String username){
        if(currentGame == null){
            return null; 
        }

        User user = userManager.getUser(username); 
        PlayerGameState state = user.currentGameState;

        if(state == null || state.gameId != currentGame.gameId){
            state = new PlayerGameState(currentGame.gameId);
            user.currentGameState = state; 
        }

        long timeRemaining = currentGame.endTime - System.currentTimeMillis(); 

        return new GameInfoResponse(shuffledWords, state.correctGroups, state.errors, timeRemaining, state.score); 
    }
}
