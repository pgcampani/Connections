package client; 

import messages.JsonUtils; 
import messages.NetworkUtils; 
import messages.requests.*; 
import messages.responses.*;
import messages.Group; 

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
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

        NetworkUtils.NIOsend(socketChannel, write_b, new LoginMessage(username, password));
        String raw = NetworkUtils.NIOreceive(socketChannel, read_b);
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);

        if(response.status.equals("OK")){
            System.out.println(response.message);

            String infoRaw = NetworkUtils.NIOreceive(socketChannel, read_b);
            GameInfoResponse gameInfo = JsonUtils.GSON.fromJson(infoRaw, GameInfoResponse.class);

            if(gameInfo.status.equals("IN_PROGRESS")){
                System.out.println("Partita in corso");
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
            }
            else{
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
        System.out.print("Inserisci ID partita (-1 per partita corrente): ");
        gameId = Integer.parseInt(scanner.nextLine().trim());

        NetworkUtils.NIOsend(socketChannel, w_buffer, new RequestGameInfoMessage(gameId));

        String raw = NetworkUtils.NIOreceive(socketChannel, r_buffer); 
        GameInfoResponse response = JsonUtils.GSON.fromJson(raw, GameInfoResponse.class);

        switch(response.status){
            case "IN_PROGRESS":
                System.out.println("Partita in corso");
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
                break;
            
            case "CONCLUDED":
                System.out.println("Partita conclusa");
                System.out.println("Gruppi corretti trovati: " + response.correctCount);
                System.out.println("Errori: " + response.errors);
                System.out.println("Punteggio: " + response.score);
                System.out.println("Soluzione");
                for(Group group : response.groups){
                    System.out.println(" " + group.theme + ": " + group.words);
                }
                break;

            case "GAME_NOT_FOUND":
                System.out.println("Partita non trovata");
                break; 

            default:
                System.out.println("Errore: " + response.message); 
                break;
        }
    }
}