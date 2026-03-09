package server; 

import com.google.gson.reflect.TypeToken; 

import messages.JsonUtils; 

import java.io.*; 
import java.lang.reflect.Type; 
import java.security.cert.PKIXBuilderParameters;
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
            JsonUtils.GSON.toJson(users, writer);
        }
        catch(IOException e){
            e.printStackTrace(); 
        }
    }

    public synchronized void finalizeGame(int gameId, GameStats stats){
        for(User user: users.values()){
            PlayerGameState state = user.currentGameState;
            if(state != null && state.gameId == gameId){
                user.totalScore += state.score; 
                user.gamesPlayed++; 
                if(state.hasWon()){
                    user.gamesWon++; 
                }

                // aggiorna statische partita
                if(stats != null){
                    stats.finalizePlayer(state.hasWon(), state.score);
                }
                // reset valori
                user.currentGameState = null; 
            }
        }
        saveUsers();
    }

    public User getUser(String username){
        return users.get(username); 
    }
}
