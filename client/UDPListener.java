package client;

import messages.JsonUtils;
import messages.responses.*;

import com.google.gson.*; 

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;


public class UDPListener implements Runnable{
    private final DatagramSocket udpSocket; 

    public UDPListener(DatagramSocket udpSocket){
        this.udpSocket = udpSocket; 
    }

    @Override
    public void run(){
        try(DatagramSocket socket = udpSocket){
            while(!Thread.currentThread().isInterrupted()){
                byte[] buffer = new byte[65535];

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);                
                JsonObject obj = JsonUtils.GSON.fromJson(json, JsonObject.class);
                String type = obj.get("type").getAsString();

                if(type.equals("GAME_END")){
                    GameEndNotification notification = JsonUtils.GSON.fromJson(json, GameEndNotification.class);
                    printGameEnd(notification);
                }
                else if(type.equals("NEW_GAME")){
                    NewGameNotification notification = JsonUtils.GSON.fromJson(json, NewGameNotification.class);
                    printNewGame(notification); 
                }
            }
        }
        catch(Exception e){
            System.err.println("Erorre UDP listener: " + e.getMessage());
        }
    }

    private void printGameEnd(GameEndNotification notification){
        System.out.println("\n=== PARTITA TERMINATA ===");

        if(notification.gameInfo != null){
            if(notification.gameInfo.status.equals("CONCLUDED")){
                System.out.println("Gruppi trovati: " + notification.gameInfo.correctCount);
                System.out.println("Errori: " + notification.gameInfo.errors);
                System.out.println("Punteggio: " + notification.gameInfo.score);
            }
            else{
                System.out.println(notification.gameInfo.message); 
            }
        }

        // Statistiche partita
        System.out.println("===========================");
        System.out.println("=== Statistiche partita ===");
        System.out.println("Giocatori partecipanti: " + notification.gameStats.playerCount);
        System.out.println("Giocatori che hanno terminato: " + notification.gameStats.playerFinished);
        System.out.println("Vincitori: " + notification.gameStats.playerWinners);
        System.out.println("Punteggio medio: " + notification.gameStats.avgPointsGame);
        System.out.println("===========================");

        // Classifica
        System.out.println("=== Classifica partita ===");

        for(LeaderboardEntry entry : notification.ranking){
            System.out.printf("%d. %-15s %d punti%n", entry.rank, entry.username, entry.score);
        }
        System.out.println("===========================");
    }

    private void printNewGame(NewGameNotification notification){
        System.out.println("=== NUOVA PARTITA INIZIATA ===");
        System.out.println("ID partita: " + notification.gameInfo.gameId);
        System.out.println("Tempo rimanente: " +  notification.gameInfo.timeRemaining / 1000);
        System.out.println("Parole: ");
        for(int i = 0; i < notification.gameInfo.remainingWords.size(); i++){
            System.out.printf("%-15s", notification.gameInfo.remainingWords.get(i));
            if((i+1) % 4 == 0){
                System.out.println();
            }
        }
        System.out.println("===========================");
        System.out.print(">  "); 
    }

}
