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
import java.util.concurrent.ConcurrentHashMap;

public class GameManager{
    private final UserManager userManager; 
    private final GameLoader gameLoader; 
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); 
    private final int gameDuration; 
    private Game currentGame; 
    private List<String> shuffledWords; 
    private ConcurrentHashMap<Integer, GameStats> gameStats = new ConcurrentHashMap<>(); 
    private final String stateFile; 

    public GameManager(UserManager userManager, String gameFile, int gameDuration , int startIndex, ConcurrentHashMap<Integer, GameStats> gameStats, String stateFile) throws IOException{
        this.userManager = userManager; 
        this.gameLoader = new GameLoader(gameFile, startIndex);
        this.gameDuration = gameDuration; 
        if(gameStats != null){
            this.gameStats = gameStats; 
        }
        this.stateFile = stateFile; 
    }

    public void start(){
        scheduler.scheduleAtFixedRate(this::startNewGame, 0, gameDuration, TimeUnit.SECONDS);
    }

    private synchronized void startNewGame(){

        // Chiudo la partita precedente e aggiorna i valori dei giocatori
        endGame(); 

        try{
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

            // Statistiche per nuova partita se non presente in mappa
            GameStats stats = gameStats.computeIfAbsent(game.gameId, id->new GameStats(id));
            stats.groups = game.groups; 

            userManager.addLoggedUsersToGame(game.gameId, stats);

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
            JsonUtils.GSON.toJson(new GameManagerState(gameLoader.getNextGameIndex(), gameStats), writer);
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

    public synchronized GameInfoResponse getGameInfo(String username, int gameId){
    
        if(gameId == -1 || (currentGame != null && currentGame.gameId == gameId)){
            // partita corrente
            if(currentGame == null){
                return GameInfoResponse.error("NO_GAME", "Nessuna partita in corso"); 
            }

            User user = userManager.getUser(username); 
            PlayerGameState state = user.currentGameState;

            if(state == null || state.gameId != currentGame.gameId){
                state = new PlayerGameState(currentGame.gameId);
                user.currentGameState = state; 
                gameStats.computeIfAbsent(currentGame.gameId, GameStats::new).addPlayer(); 
            }

            long timeRemaining = currentGame.endTime - System.currentTimeMillis(); 

            List<String> remainingWords = new ArrayList<>(shuffledWords); 
            for(Group group : state.correctGroups){
                remainingWords.removeAll(group.words);
            }

            return GameInfoResponse.inProgress(timeRemaining, state.correctGroups, remainingWords, state.errors, state.score, currentGame.gameId);
        } 

        // partita passata
        GameStats stats = gameStats.get(gameId);
        if(stats == null){
            return GameInfoResponse.error("GAME_NOT_FOUND", "Partita non trovata");
        }

        User targetUser = userManager.getUser(username);
        PlayerGameState pastState = targetUser.pastGames.get(gameId);
        if(pastState == null){
            return GameInfoResponse.error("NOT_PLAYED", "Non hai partecipato a questa partita");
        }

        return GameInfoResponse.concluded(stats.groups, pastState.correctGroups.size(), pastState.errors, pastState.score); 
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
                    // Se ho vinto il quarto gruppo viene dato in automatico e aggiunto alla lista dei gruppi indovinati
                    for(Group remaining : currentGame.groups){
                        if(!state.correctGroups.contains(remaining)){
                            state.correctGroups.add(remaining);
                            break; 
                        }
                    }
                    state.finished = true;
                    
                    GameStats stats = gameStats.get(currentGame.gameId);
                    if(stats != null){
                        stats.finalizePlayer(true, state.score, state.finished);
                    }
                    
                    return "WON";
                }

                return "CORRECT";
            }
        }
        state.errors++; 
        state.score -= 4; 

        if(state.hasLost()){
            state.finished = true;
            GameStats stats = gameStats.get(currentGame.gameId);
            if(stats != null){
                stats.finalizePlayer(false, state.score, state.finished);
            }
            return "LOST";
        }

        return "WRONG"; 
    }

    public void endGame(){
        if(currentGame == null){
            return;
        }
        GameStats stats = gameStats.computeIfAbsent(currentGame.gameId, GameStats::new);
        stats.groups = currentGame.groups; 
        userManager.finalizeGame(currentGame.gameId, stats);
        stats.conculded = true;
        saveState(stateFile);
    }

    public synchronized GameStatsResponse getGameStats(int gameId){

        if(currentGame == null){
            return GameStatsResponse.error("NO_GAME", "Nessuna partita in corso");
        }

        if(currentGame != null && currentGame.gameId == gameId){

            GameStats stats = gameStats.get(currentGame.gameId); 
            if(stats == null){
                long timeRemaining = currentGame.endTime - System.currentTimeMillis();
                return GameStatsResponse.inProgress(timeRemaining, 0, 0, 0);
            }

            long timeRemaining = currentGame.endTime - System.currentTimeMillis(); 

            return GameStatsResponse.inProgress(timeRemaining, stats.getActivePlayers(), stats.finishedPlayers, stats.wonPlayers);
        }

        GameStats stats = gameStats.get(gameId); 
        if(stats == null){
            return GameStatsResponse.error("NO_GAME", "Partita non trovata"); 
        }

        return GameStatsResponse.concluded(stats.totalPlayers, stats.finishedPlayers, stats.wonPlayers, stats.getAverageScore()); 
    }
}
