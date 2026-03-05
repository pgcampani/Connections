package server.game; 

import messages.responses.*;
import messages.*;
import server.User; 
import server.UserManager;

import java.io.IOException;
import java.util.Collections; 
import java.util.List;
import java.util.ArrayList; 
import java.util.Collection;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.Executors; 
import java.util.concurrent.ScheduledExecutorService; 
import java.util.concurrent.TimeUnit;
import java.io.FileWriter; 

public class GameManager{
    private final UserManager userManager; 
    private final GameLoader gameLoader; 
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); 
    private final int gameDuration; 
    private Game currentGame; 
    private List<String> shuffledWords; 

    public GameManager(UserManager userManager, String gameFile, int gameDuration , int startIndex) throws IOException{
        this.userManager = userManager; 
        this.gameLoader = new GameLoader(gameFile, startIndex);
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

    public synchronized void saveState(String stateFile){
        try(FileWriter writer = new FileWriter(stateFile)){
            JsonUtils.GSON.toJson(new GameManagerState(gameLoader.getNextGameIndex()), writer);
        } catch(IOException e){
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

    public synchronized String submitProposal(String username, List<String> words){
        if(currentGame == null){
            return "NO_GAME"; 
        }

        User user = userManager.getUser(username); 
        PlayerGameState state = user.currentGameState;

        if(state == null || state.gameId != currentGame.gameId){
            return "NO_GAME"; 
        } 

        if(state.finished){
            return "GAME_FINISHED";
        }

        if(words.size() != 4){
            return "INVALID_PROPOSAL";
        }

        List<String> allWords = new ArrayList<>();

        // Controllo delle parole in partita corrente
        for(Group group : currentGame.groups){
            allWords.addAll(group.words);
        }

        if(!allWords.containsAll(words)){
            return "INVALID_PROPOSAL";
        }

        if(System.currentTimeMillis() > currentGame.endTime){
            return "GAME_FINISHED";
        }

        for(Group correctGroup : state.correctGroups){
            if(correctGroup.words.containsAll(words)){
                return "INVALID_PROPOSAL";
            }
        }

        for(Group group : currentGame.groups){
            if(group.words.containsAll(words) && words.containsAll(group.words)){
                
                // corretta
                state.correctGroups.add(group);
                state.score += 6;

                System.out.println("Punteggio corrente di " + username + " e' " + state.score);

                if(state.hasWon()){
                    state.finished = true;
                    return "WON";
                }

                return "CORRECT";
            }
        }
        state.errors++; 
        state.score -= 4; 

        if(state.hasLost()){
            state.finished = true;
            return "LOST";
        }
        return "WRONG"; 
    }

    private int getBonus(int correctCount){
        switch(correctCount){
            case 1: return 6;
            case 2: return 6;
            case 3: return 6;
            default: return 0; 
        }
    }
}
