package server; 

import com.google.gson.reflect.TypeToken; 

import messages.JsonUtils; 

import java.io.*; 
import java.lang.reflect.Type; 
import java.util.List; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map; 
import java.util.concurrent.ConcurrentHashMap; 

import server.game.PlayerGameState;
import server.game.GameStats; 

public class UserManager{
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(); 
    private final String usersFile; 

    public UserManager(String usersFile){
        this.usersFile = usersFile; 
        loadUsers(); 
    }

    public boolean register(String username, String password){
        
        User newUser = new User(password); 
        newUser.username = username; 
        User existing = users.putIfAbsent(username, newUser);

        if(existing != null){
            // username già registrato
            return false; 
        }
        saveUsers(); // Salvo immediatamente l'utente nel file JSON
        return true; 
    }

    public String login(String username, String password){
        
        User user = users.get(username); 

        if(user == null){
            return "USER_NOT_FOUND"; 
        }

        synchronized(user){
            if(user.isLogged){
                return "USER_ALREADY_LOGGED"; 
            }
            if(!user.password.equals(password)){
                return "WRONG_PASSWORD"; 
            }
            user.isLogged = true; 
        }
        return "OK"; 
    }

    public void logout(String username){
        User user = users.get(username); 

        if(user == null) return; 

        synchronized (user){
            user.currentGameid = -1;
            user.isLogged = false;  
        }
    }

    public void logoutAll(){
        for(User user : users.values()){
            synchronized(user){
                user.isLogged = false;
                user.currentGameid = -1; 
            }
        }
    }


    public String updateCredential(String loggedUsername, String old_username, String old_psw, String new_username, String new_psw){
        
        synchronized(this){
            // lock mappa
            User user = users.get(old_username); 

            if(user == null){
                return "USER_NOT_FOUND"; 
            }

            synchronized(user){
                // lock utente
                
                if(user.isLogged && !old_username.equals(loggedUsername)) return "USER_ALREADY_LOGGED";

                if(!user.password.equals(old_psw)){
                    return "WRONG_PASSWORD";
                }

                if(new_username == null || new_username.equals(old_username) && new_psw != null){
                    user.password = new_psw; 
                }
                else if(new_username != null && new_psw == null){
                    User existing = users.putIfAbsent(new_username, user);
                    if(existing != null){
                        return "USERNAME_TAKEN";
                    }
                    users.remove(old_username);
                }
                else if(new_username != null && new_psw != null){
                    User existing = users.putIfAbsent(new_username, user);
                    if(existing != null){
                        return "USERNAME_TAKEN";
                    }
                    user.password = new_psw;
                    users.remove(old_username);
                }
                
                else{
                    return "NO_UPDATE"; 
                }
            }
        }
        saveUsers();
        return "OK"; 
    }

    private void loadUsers(){
        File file = new File(usersFile); 
        if(!file.exists()) return; 

        try(FileReader reader = new FileReader(file)){
            Type type = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType(); 
            
            ConcurrentHashMap<String, User> loaded = JsonUtils.GSON.fromJson(reader, type); 
            
            // Se il file non è vuoto scrivi nella mappa
            if(loaded != null){
                for(Map.Entry<String, User> entry : loaded.entrySet()){
                    entry.getValue().username = entry.getKey(); 
                }
                users.putAll(loaded); 
            }
            System.out.println("Utenti caricati: " + users.size()); 
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public synchronized void saveUsers(){
        try(FileWriter writer = new FileWriter(usersFile)){
            JsonUtils.GSON_PRETTY.toJson(users, writer);
        }
        catch(IOException e){
            e.printStackTrace(); 
        }
    }

    public synchronized void finalizeGame(int gameId, GameStats stats){
        for(User user: users.values()){
            PlayerGameState state = user.currentGameState;
            if(state != null && state.gameId == gameId){
                if(!state.finished){
                    user.totalScore += state.score; 
                    user.gamesPlayed++; 
                    user.currentStreak = 0; 
                    user.mistakeHistogram[5]++;
                     // aggiorna statische partita
                    if(stats != null){
                        if(!state.finished){
                            stats.finalizePlayer(state.hasWon(), state.score, state.finished);
                        }
                    } 
                }
               
                state.finished = true;
                // salvataggio e reset valori
                user.pastGames.put(state.gameId, state); 
                user.currentGameState = null; 
            }
        }
        saveUsers();
    }

    public synchronized void finalizeWin(String username, PlayerGameState state){
        User user = users.get(username);
        if(user == null) return;

        user.totalScore += state.score;
        user.gamesPlayed++; 
        user.gamesWon++; 
        user.currentStreak++;
        if(user.currentStreak > user.maxStreak){
            user.maxStreak = user.currentStreak; 
        }
        if(state.errors == 0){
            user.perfectPuzzles++; 
        }
        user.mistakeHistogram[state.errors]++; 

    }


    public synchronized void finalizeLoss(String username, PlayerGameState state){
        User user = users.get(username);
        if(user == null) return;

        user.totalScore += state.score;
        user.gamesPlayed++; 
        user.gameLost++;
        user.currentStreak = 0; 
        user.mistakeHistogram[state.errors]++; 
    }

    public void addLoggedUsersToGame(int gameId, GameStats stats){
        for(User user : users.values()){
            if(user.isLogged){
                user.currentGameState = new PlayerGameState(gameId);
                stats.addPlayer();
            }
        }
    }

    public List<User> getLeaderboard(String playerName, int topPlayers){
        List<User> leaderboard = new ArrayList<>(users.values());
        Collections.sort(leaderboard, (a,b) -> b.totalScore - a.totalScore);
        
        if(playerName != null){
            for(int i = 0; i < leaderboard.size(); i++){
                if(leaderboard.get(i).username != null && leaderboard.get(i).username.equals(playerName)){
                    return leaderboard.subList(i, i+1);
                }
            }
            return null; 
        }

        int limit; 
        if(topPlayers == -1){
            limit = leaderboard.size();
        }
        else{
            limit = Math.min(topPlayers, leaderboard.size());
        }
        
        return leaderboard.subList(0, limit); 
    }

    public User getUser(String username){
        return users.get(username); 
    }
}
