package server; 

import com.google.gson.reflect.TypeToken; 

import messages.JsonUtils; 

import java.io.*; 
import java.lang.reflect.Type; 
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map; 
import java.util.concurrent.ConcurrentHashMap; 

import messages.responses.LeaderboardEntry;
import server.game.PlayerGameState;
import server.game.GameStats; 

public class UserManager{
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(); 
    private final String usersFile; 
    private final ConcurrentHashMap<String, SocketAddress> udpClients = new ConcurrentHashMap<>();

    public UserManager(String usersFile){
        this.usersFile = usersFile; 
        loadUsers(); 
    }

    public boolean register(String username, String password){
        
        User newUser = new User(password); 
        newUser.username = username; 
        // Inseriamo solo se la chiave non esiste già
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

        unregisterUdpClient(username);
    }

    // Logout di tutti gli utenti - per shutdown del server
    public void logoutAll(){
        for(User user : users.values()){
            synchronized(user){
                user.isLogged = false;
                user.currentGameid = -1; 
            }
            unregisterUdpClient(user.username);
        }
    }


    public String updateCredential(String loggedUsername, String old_username, String old_psw, String new_username, String new_psw){    
        if(loggedUsername == null || !loggedUsername.equals(old_username)){
            return "UNAUTHORIZED";
        }

        User user = users.get(old_username);
        if (user == null) return "USER_NOT_FOUND";

        synchronized (user) {
            // 2. Controllo password attuale
            if(!user.password.equals(old_psw)){
                return "WRONG_PASSWORD";
            }
            // Caso 1: Cambio SOLO password (new_username è null o uguale al vecchio)
            if(new_username == null || new_username.equals(old_username)){
                if(new_psw != null){
                    user.password = new_psw;
                }
                else{
                    return "NO_UPDATE"; // Nessun dato nuovo fornito
                }
            } 
            else{
                // Tenta di occupare il nuovo nome in modo atomico sulla mappa
                User existing = users.putIfAbsent(new_username, user);
                if(existing != null){
                    return "USERNAME_TAKEN";
                }
                // Rinomina riuscita: liberiamo il vecchio nome
                users.remove(old_username);
                user.username = new_username;

                // Aggiorna anche indirizzo UDP a cui inviare notifiche asaincrone
                SocketAddress clientAddress = udpClients.remove(old_username);
                if(clientAddress != null){
                    udpClients.put(new_username, clientAddress); 
                }

                if(new_psw != null){
                    user.password = new_psw;
                }
            }
        }
        // Salvataggio persistente
        saveUsers();
        return "OK";
    }

    // Carica utenti da file JSON 
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

    // Finaliza tutti i giocatori che ancora non hanno terminato (non hanno vinto/perso)
    public synchronized void finalizeGame(int gameId, GameStats stats){
        for(User user: users.values()){
            PlayerGameState state = user.currentGameState;
            if(state != null && state.gameId == gameId){
                if(!state.finished){
                    user.totalScore += state.score; 
                    user.gamesPlayed++; 
                    user.currentStreak = 0; 
                    user.mistakeHistogram[5]++;
                     // aggiorna statistiche partita
                    if(stats != null){
                        stats.finalizePlayer(state.hasWon(), state.score, state.finished);
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

    // Aggiornamento statistiche per utente che vince una partita
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

    // Aggiornamento statistiche per utente che perde una partita
    public synchronized void finalizeLoss(String username, PlayerGameState state){
        User user = users.get(username);
        if(user == null) return;

        user.totalScore += state.score;
        user.gamesPlayed++; 
        user.gameLost++;
        user.currentStreak = 0; 
        user.mistakeHistogram[state.errors]++; 
    }

    // Crea un nuovo PlayerGameState per gli utenti rimasti loggati all'avvio di
    // una nuova partita
    public synchronized void addLoggedUsersToGame(int gameId, GameStats stats){
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
            // vogliamo prendere la leaderboard globale
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

    // UDP
    public void registerUdpClient(String username, InetAddress address, int udpPort){
        udpClients.put(username, new InetSocketAddress(address, udpPort));
    }

    public void unregisterUdpClient(String username){
        udpClients.remove(username);
    }

    public ConcurrentHashMap<String, SocketAddress> getUdpClients(){
        return udpClients; 
    }

    // Stila la classifica per una partita singola
    public List<LeaderboardEntry> getGameRanking(int gameId){
        List<Map.Entry<String, User>> players = new ArrayList<>(); 
        for(Map.Entry<String, User> entry : users.entrySet()){
            PlayerGameState state = entry.getValue().pastGames.get(gameId);
            if(state != null){
                players.add(entry); 
            } 
        }

        // Ordine per punteggio decrescente
        Collections.sort(players, (a,b) -> b.getValue().pastGames.get(gameId).score - a.getValue().pastGames.get(gameId).score);

        // Classifica - posizione. username. score.
        List<LeaderboardEntry> ranking = new ArrayList<>(); 
        for(int i = 0; i < players.size(); i++){
            Map.Entry<String, User> entry = players.get(i); 
            PlayerGameState state = entry.getValue().pastGames.get(gameId);
            ranking.add(new LeaderboardEntry(i+1, entry.getKey(), state.score));
        }
        return ranking;
    }
}
