package server; 

import com.google.gson.JsonParser; 

import messages.JsonUtils; 
import messages.NetworkUtils; 
import messages.responses.*; 
import messages.requests.*;
import server.game.GameManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Gestisce la comunicazione con un singolo client connesso
// Per la comunicazione, ogni messaggio è un oggetto JSON terminato da \n
public class ClientHandler implements Runnable{

    private final Socket clientSocket;
    private final UserManager userManager; 
    private final GameManager gameManager; 

    public ClientHandler(Socket socket, UserManager userManager, GameManager gameManager){
        this.clientSocket = socket;
        this.userManager = userManager; 
        this.gameManager = gameManager; 
    }   

    @Override
    public void run(){

        String loggedUsername = null; 

        try(Socket socket = clientSocket;
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)), true);
        ){
            String message;
            // Loop principale - legge messaggi finché la connessione è aperta
            while((message = NetworkUtils.TCPreceive(in)) != null){

                String operation = JsonParser.parseString(message).getAsJsonObject().get("operation").getAsString(); 

                switch(operation){
                    case "register": 
                        handleRegister(message, out); 
                        break; 
                    
                    case "login":
                        loggedUsername = handleLogin(message, out); 
                        break; 
                    
                    case "logout": 
                        handleLogout(loggedUsername, out); 
                        loggedUsername = null; 
                        break; 
                    
                    case "updateCredential":
                        loggedUsername = handleUpdateCredential(loggedUsername, message, out);
                        break; 
                    
                    case "submitProposal": 
                        handleProposal(loggedUsername, message, out); 
                        break; 

                    case "requestGameInfo":
                        handleGameInfo(loggedUsername, message, out); 
                        break; 

                    case "requestGameStats":
                        handleGameStats(message, out); 
                        break; 
                    
                    case "requestLeaderboard":
                        handleLeaderboard(loggedUsername, message, out);
                        break; 

                    case "requestPlayerStats":
                        handlePlayerStats(loggedUsername, message, out); 
                        break; 
                    
                    default: 
                        NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Operazione non riconosciuta")); 
                        break; 
                }
            }

            System.out.println("Client disconnesso."); 
        }
        catch(SocketException se){
            // Disconnessione improvvisa del client
            System.out.println("Client disconnesso."); 
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally{
            if(loggedUsername != null){
                // Logout automatico se client si disconnette senza fare logout
                userManager.logout(loggedUsername);
                System.out.println("Logout automatico effettuato per " + loggedUsername); 
            }
        }
    }

    // Gestisce la registrazione di un nuovo utente
    private void handleRegister(String message, PrintWriter out){
        RegisterMessage request = JsonUtils.GSON.fromJson(message, RegisterMessage.class);

        if(userManager.register(request.name.trim(), request.password.trim())){
            NetworkUtils.TCPsend(out, new ServerResponse("OK", "Registrazione avvenuta con successo"));
        }
        else{
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Username occupato"));
        }
    }

    // Gestisce login di un utente - invia info partita corrente e registra indirizzo UDP
    private String handleLogin(String message, PrintWriter out){
        LoginMessage request = JsonUtils.GSON.fromJson(message, LoginMessage.class);
        String result = userManager.login(request.username.trim(), request.password.trim()); 

       switch(result){
        case "OK": 
            NetworkUtils.TCPsend(out, new ServerResponse("OK", "Login effettuato con successo"));
            // Registra inidirizzo UDP per notifiche asincrone
            userManager.registerUdpClient(request.username.trim(), clientSocket.getInetAddress(), request.udpPort);
            GameInfoResponse gameInfo = gameManager.getGameInfo(request.username.trim(), -1);   // -1 partita corrente 
            NetworkUtils.TCPsend(out, gameInfo);
            return request.username.trim(); 
        
        case "USER_NOT_FOUND": 
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Utente inesistente"));
            break;

        case "WRONG_PASSWORD": 
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Password errata"));
            break; 

        case "USER_ALREADY_LOGGED":
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Utente gia' connesso"));
            break; 
                
        default: 
            break; 
       }

       return null; 
    }

    // Gestisce logout utente
    private void handleLogout(String loggedUsername, PrintWriter out){
        if(loggedUsername == null){
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Nessun utente loggato"));
            return; 
        }
        userManager.logout(loggedUsername); 
        NetworkUtils.TCPsend(out, new ServerResponse("OK", "Logout avvenuto con successo"));
    }

    // Aggiornamento credenziali 
    private String handleUpdateCredential(String loggedUsername, String message, PrintWriter out){
        if(loggedUsername == null){
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Nessun utente loggato"));
            return null;
        }

        UpdateCredentialMessage request = JsonUtils.GSON.fromJson(message, UpdateCredentialMessage.class);

        String result = userManager.updateCredential(loggedUsername, request.old_username, request.old_psw, request.new_username, request.new_psw);

        switch(result){
            case "OK":
                NetworkUtils.TCPsend(out, new ServerResponse("OK", "Credenziali aggiornate"));
                if(request.new_username != null){
                    return request.new_username;
                }
                else{
                    return loggedUsername; 
                }
            
            case "UNAUTHORIZED":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Utente non autorizzato"));
                break;

            case "WRONG_PASSWORD":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Password errata"));
                break;

            case "USERNAME_TAKEN":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Username occupato"));
                break; 

            case "USER_NOT_FOUND":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Username inesistente"));
                break;

            case "USER_ALREADY_LOGGED":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Utente gia' loggato"));
                break; 

            default:
                break;
        }

        return loggedUsername; 
    }

    // Gestisce l'invio di una proposta di 4 parole
    private void handleProposal(String loggedUsername, String message, PrintWriter out){
        
        if(loggedUsername == null){
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Devi essere loggato"));
            return;
        }

        SubmitProposalMessage proposalMessage = JsonUtils.GSON.fromJson(message, SubmitProposalMessage.class); 
        String result = gameManager.submitProposal(loggedUsername, proposalMessage.words);

        switch(result){
            case "CORRECT":
            NetworkUtils.TCPsend(out, new ServerResponse("OK", "Proposta corretta!"));
            break;

            case "WON":
                NetworkUtils.TCPsend(out, new ServerResponse("OK", "Hai vinto la partita!"));
                break;

            case "WRONG":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Proposta sbagliata"));
                break;

            case "LOST":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Hai perso la partita!"));
                break;

            case "INVALID_PROPOSAL":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Proposta non valida"));
                break;

            case "NO_GAME":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Nessuna partita in corso"));
                break;

            case "GAME_FINISHED":
                NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Hai già terminato questa partita"));
                break;

            default:
                break;

        }
    }

    // Gestisce richieste di informazioni su partita specifica (in corso o già terminata)
    private void handleGameInfo(String loggedUsername, String message, PrintWriter out){
        if(loggedUsername == null){
            NetworkUtils.TCPsend(out, GameInfoResponse.error("ERROR", "Devi essere loggato"));
            return;
        }

        RequestGameInfoMessage request = JsonUtils.GSON.fromJson(message, RequestGameInfoMessage.class);
        GameInfoResponse response = gameManager.getGameInfo(loggedUsername, request.gameId); 
        NetworkUtils.TCPsend(out, response);
    }

    // Gestisce richieste di statistiche relative ad una specifica partita (in corso o terminata)
    private void handleGameStats(String message, PrintWriter out){

        RequestGameStatsMessage request = JsonUtils.GSON.fromJson(message, RequestGameStatsMessage.class); 

        GameStatsResponse response = gameManager.getGameStats(request.gameId); 
        NetworkUtils.TCPsend(out, response);
    }
    
    // Gestione della classifica globale (completa, top K utenti o posizione di utente specifico)
    private void handleLeaderboard(String loggedUsername, String message, PrintWriter out){
        if(loggedUsername == null){
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Devi essere loggato"));
            return; 
        }

        RequestLeaderboardMessage request = JsonUtils.GSON.fromJson(message, RequestLeaderboardMessage.class);
        
        // Lista di entry con posizione calcolata in base alla posizione nella lista ordinata
        List<User> sortedLeaderboard = userManager.getLeaderboard(request.playerName, request.topPlayers);

        if(sortedLeaderboard == null){
            LeaderboardResponse error = new LeaderboardResponse();
            error.status = "PLAYER_NOT_FOUND";
            error.message = "Giocatore non trovato"; 
            NetworkUtils.TCPsend(out, error);
            return; 
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for(int i = 0; i < sortedLeaderboard.size(); i++){
            entries.add(new LeaderboardEntry(i + 1, sortedLeaderboard.get(i).username, sortedLeaderboard.get(i).totalScore));
        }

        LeaderboardResponse response = new LeaderboardResponse();
        response.status = "OK";
        response.entries = entries;
        NetworkUtils.TCPsend(out, response);
    }

    // Gestione statistiche personali del giocatore loggato
    private void handlePlayerStats(String loggedUsername, String message, PrintWriter out){
        if(loggedUsername == null){
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Devi essere loggato"));
            return;
        }
        
        User user = userManager.getUser(loggedUsername); 

        PlayerStatsResponse stats = new PlayerStatsResponse(); 
        stats.status = "OK"; 
        stats.puzzlesCompleted = user.gamesPlayed; 
        if(user.gamesPlayed == 0){
            stats.winRate = 0; 
            stats.lossRate = 0; 
        }
        else{
            stats.winRate = (double) user.gamesWon / user.gamesPlayed * 100; 
            stats.lossRate = (double) user.gameLost / user.gamesPlayed * 100;
        }
        stats.currentStreak = user.currentStreak; 
        stats.maxStreak = user.maxStreak; 
        stats.perfectPuzzles = user.perfectPuzzles; 
        stats.mistakeHistogram = user.mistakeHistogram; 

        NetworkUtils.TCPsend(out, stats);
    }
}