package client; 

import messages.JsonUtils; 
import messages.NetworkUtils; 
import messages.requests.*; 
import messages.responses.*;
import messages.Group; 

import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.List; 
import java.util.ArrayList; 

public class ClientCommands{
    public static void  handleRegister(Scanner scanner, SocketChannel socketChannel, ByteBuffer write_b, ByteBuffer read_b) throws IOException{
        System.out.print("Username ");
        String username = scanner.nextLine().trim(); 
        System.out.print("Password "); 
        String password = scanner.nextLine().trim();
        
        if(username.isEmpty() || password.isEmpty()){
            System.out.println("Errore: i campi Username e Password non possono essere vuoti");
            return; 
        }

        NetworkUtils.NIOsend(socketChannel, write_b, new RegisterMessage(username, password));
        String raw = NetworkUtils.NIOreceive(socketChannel, read_b); 
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);

        if(response.status.equals("OK")){
            System.out.println(response.message);
        }
        else{
            System.out.println("Errore: " + response.message);
        } 
    }

    public static boolean handleLogin(Scanner scanner, SocketChannel socketChannel, ByteBuffer write_b, ByteBuffer read_b) throws IOException{
        System.out.print("Username ");
        String username = scanner.nextLine().trim(); 
        System.out.print("Password "); 
        String password = scanner.nextLine().trim();  

        if(username.isEmpty() || password.isEmpty()){
            System.out.println("Errore: i campi Username e Password non possono essere vuoti");
            return false; 
        }

        DatagramSocket udpSocket = new DatagramSocket(0); 
        int udpPort = udpSocket.getLocalPort(); 

        NetworkUtils.NIOsend(socketChannel, write_b, new LoginMessage(username, password, udpPort));
        String raw = NetworkUtils.NIOreceive(socketChannel, read_b);
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);

        if(response.status.equals("OK")){ 
            System.out.println(response.message);

            String infoRaw = NetworkUtils.NIOreceive(socketChannel, read_b);
            GameInfoResponse gameInfo = JsonUtils.GSON.fromJson(infoRaw, GameInfoResponse.class);

             // Avvio thread UDPListener
            Thread udpThread = new Thread(new UDPListener(udpSocket));
            udpThread.setDaemon(true);
            udpThread.start();

            if(gameInfo.status.equals("IN_PROGRESS")){
                System.out.println("\n=== PARTITA IN CORSO ===");
                System.out.println("ID partita: " + gameInfo.gameId);
                System.out.println("Parole:");
                for(int i = 0; i < gameInfo.remainingWords.size(); i++){
                    System.out.printf("%-15s", gameInfo.remainingWords.get(i));
                    if((i + 1) % 4 == 0){
                        // Dopo 4 parole stampate vado a capo
                        System.out.println();
                    }
                }
                System.out.println("Errori: " + gameInfo.errors);
                System.out.println("Punteggio: " + gameInfo.score);
                System.out.println("Tempo rimanente: " + gameInfo.timeRemaining / 1000 + " secondi");
                System.out.println("==========================="); 
            }
            else{
                udpSocket.close(); 
                System.out.println(gameInfo.message); 
            }
            return true;
        }
        else{
            System.out.println("Errore: " + response.message);
            return false;
        }
    }

    public static boolean handleLogout(SocketChannel socketChannel, ByteBuffer write_b, ByteBuffer read_b) throws IOException{
        NetworkUtils.NIOsend(socketChannel, write_b, new LogoutMessage());

        String raw = NetworkUtils.NIOreceive(socketChannel, read_b); 
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class); 

        if(response.status.equals("OK")){
            System.out.println("Logged out successfully"); 
            return true;
        }
        else{
            System.out.println("Errore nel logout"); 
            return false; 
        }
    }

    public static void updateCredential(Scanner scanner, SocketChannel socketChannel, ByteBuffer write_b, ByteBuffer read_b) throws IOException{
        System.out.print("Username ");
        String old_username = scanner.nextLine().trim();
        System.out.print("Password "); 
        String old_password = scanner.nextLine().trim();
        System.out.print("Nuovo Username ");
        String new_username = scanner.nextLine().trim(); 
        System.out.print("Nuova Password ");
        String new_password = scanner.nextLine().trim();
        
        if(old_username.isEmpty() || old_password.isEmpty() || new_username.isEmpty() || new_password.isEmpty()){
            System.out.println("Errore: i campi Username e Password non possono essere vuoti");
            return;
        }

        NetworkUtils.NIOsend(socketChannel, write_b, new UpdateCredentialMessage(old_username, old_password, new_username, new_password));

        String raw = NetworkUtils.NIOreceive(socketChannel, read_b); 
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);

        if(response.status.equals("OK")){
            System.out.println(response.message);
        }
        else{
            System.out.println("Errore " + response.message);
        }
    }

    public static void submitProposal(Scanner scanner, SocketChannel socketChannel, ByteBuffer w_buffer, ByteBuffer r_buffer) throws IOException{
        List<String> proposal = new ArrayList<>(); 
        System.out.print("Inserisci 4 parole separate da virgola ");
        String input = scanner.nextLine().trim().toUpperCase(); 

        String[] parts = input.split(","); 

        if(parts.length != 4){
            System.out.println("Errore: inserire esattamente 4 parole"); 
            return; 
        }

        for(String word : parts){
            proposal.add(word.trim()); 
        }

        NetworkUtils.NIOsend(socketChannel, w_buffer, new SubmitProposalMessage(proposal));
        String raw = NetworkUtils.NIOreceive(socketChannel, r_buffer);
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);
        System.out.println(response.message); 
    }


    public static void handleGameInfo(Scanner scanner, SocketChannel socketChannel, ByteBuffer w_buffer, ByteBuffer r_buffer) throws IOException{
        int gameId; 
        System.out.print("Inserisci ID partita: ");
        gameId = Integer.parseInt(scanner.nextLine().trim());

        NetworkUtils.NIOsend(socketChannel, w_buffer, new RequestGameInfoMessage(gameId));

        String raw = NetworkUtils.NIOreceive(socketChannel, r_buffer); 
        GameInfoResponse response = JsonUtils.GSON.fromJson(raw, GameInfoResponse.class);

        switch(response.status){
            case "IN_PROGRESS":
                System.out.println("\n=== GAME INFO ===");
                System.out.println("Partita in corso. ID partita: " + response.gameId);
                System.out.println("Tempo rimanente " + response.timeRemaining / 1000 + " secondi");
                System.out.println("Errori: " + response.errors);
                System.out.println("Punteggio: " + response.score);
                System.out.println("Gruppi corretti trovati: ");
                for(Group group : response.correctGroups){
                    System.out.println(" " + group.theme + ": " + group.words);
                }
                System.out.println("Parole rimanenti: ");
                for(int i = 0; i < response.remainingWords.size(); i++){
                    System.out.printf("%-15s", response.remainingWords.get(i));
                    if((i + 1) % 4 == 0){
                        System.out.println();
                    } 
                }
                System.out.println("==========================="); 
                break;
            
            case "CONCLUDED":
                System.out.println("\n=== GAME INFO ==="); 
                System.out.println("Partita conclusa");
                System.out.println("Gruppi corretti trovati: " + response.correctCount);
                System.out.println("Errori: " + response.errors);
                System.out.println("Punteggio: " + response.score);
                System.out.println("Soluzione");
                for(Group group : response.groups){
                    System.out.println(" " + group.theme + ": " + group.words);
                }
                System.out.println("==========================="); 
                break;

            case "GAME_NOT_FOUND":
                System.out.println("Partita non trovata");
                break; 

            default:
                System.out.println("Errore: " + response.message); 
                break;
        }
    }


    public static void handleGameStats(Scanner scanner, SocketChannel socketChannel, ByteBuffer w_buffer, ByteBuffer r_buffer)throws IOException{
        int gameId; 
        System.out.print("Inserisci ID partita ");
        gameId = Integer.parseInt(scanner.nextLine().trim()); 

        NetworkUtils.NIOsend(socketChannel, w_buffer, new RequestGameStatsMessage(gameId));

        String raw = NetworkUtils.NIOreceive(socketChannel, r_buffer);
        GameStatsResponse response = JsonUtils.GSON.fromJson(raw, GameStatsResponse.class); 

        switch(response.status){
            case "IN_PROGRESS":
                System.out.println("\n=== GAME STATS ===");
                System.out.println("Tempo rimanente: " + response.timeRemaining / 1000 + " secondi");
                System.out.println("Giocatori in partita: " + response.playerInGame); 
                System.out.println("Giocatori che hanno terminato la partita: " + response.playerFinished);
                System.out.println("Numero vincitori: " + response.playerWinners);
                System.out.println("==========================="); 
                break; 
            
            case "CONCLUDED":
                System.out.println("\n=== GAME STATS ==="); 
                System.out.println("Giocatori che hanno giocato: " + response.playerCount); 
                System.out.println("Giocatori che hanno terminato la partita: " + response.playerFinished); 
                System.out.println("Numero vincitori: " + response.playerWinners);
                System.out.println("Media punti totale: " + response.avgPointsGame);
                System.out.println("==========================="); 
                break; 

            case "NO_GAME": 
                System.out.println("Errore: " + response.message);
                break; 
            
            default:
                System.out.println("Errore: " + response.message);
                break; 
        }
    }

    public static void handleLeaderboard(Scanner scanner, SocketChannel socketChannel, ByteBuffer w_buffer, ByteBuffer r_buffer) throws IOException{
        
        System.out.println("\nInserisci indice");
        System.out.println("1.  Classifica completa");
        System.out.println("2.  Top K giocatori");
        System.out.println("3.  Posizione di un giocatore"); 
        System.out.print(">  ");

        String playerName = null; 
        int topPlayers = -1; 
        String index = scanner.nextLine().trim(); 

        switch(index){
            case "1":
                break;
            
            case "2":
                System.out.print("Inserisci numero giocatori da visualizzare: ");
                topPlayers = Integer.parseInt(scanner.nextLine().trim());
                break; 

            case "3":
                System.out.print("Inserisci username: ");
                playerName = scanner.nextLine().trim();
                break;

            default:
                System.out.println("Indice non valido");
                return; 
        }

        NetworkUtils.NIOsend(socketChannel, w_buffer, new RequestLeaderboardMessage(playerName, topPlayers));

        String raw = NetworkUtils.NIOreceive(socketChannel, r_buffer);
        LeaderboardResponse response = JsonUtils.GSON.fromJson(raw, LeaderboardResponse.class);

        if(response.status.equals("OK")){
            System.out.println("\n=== LEADERBOARD ==="); 
            for(LeaderboardEntry entry : response.entries){
                System.out.printf("%d. %-15s %d punti%n", entry.rank, entry.username, entry.score);        
            }
            System.out.println("==========================="); 
        }
        else{
            System.out.println("Errore " + response.message); 
        }
    }


    public static void handlePlayerStats(SocketChannel socketChannel, ByteBuffer w_buffer, ByteBuffer r_buffer)throws  IOException{
        NetworkUtils.NIOsend(socketChannel, w_buffer, new RequestPlayerStatsMessage());

        String raw = NetworkUtils.NIOreceive(socketChannel, r_buffer); 
        PlayerStatsResponse response = JsonUtils.GSON.fromJson(raw, PlayerStatsResponse.class); 

        if(response.status.equals("OK")){
            System.out.println("Puzzle completati: " + response.puzzlesCompleted);
            System.out.println("Win Rate: " + response.winRate + "%");
            System.out.println("Loss Rate: " + response.lossRate + "%");
            System.out.println("Streak corrente: " + response.currentStreak);
            System.out.println("Streak più lunga: " + response.maxStreak);
            System.out.println("Puzzle perfetti: "+ response.perfectPuzzles);
            System.out.println("Istogramma errori: ");
            for(int i = 0; i < 5; i++){
                System.out.println(" " + i + " errori " + response.mistakeHistogram[i]);
            }
            System.out.println(" Non terminate: " + response.mistakeHistogram[5]);
        }
        else{
            System.out.println("Errore: " + response.message); 
        }
    }
}