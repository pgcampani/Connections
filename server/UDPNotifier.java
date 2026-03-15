package server; 

import messages.JsonUtils;

import java.io.IOException; 
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress; 
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*; 

public class UDPNotifier{

    public void notifyAll(ConcurrentHashMap<String, SocketAddress> udpClients, Object message){
        String json = JsonUtils.GSON.toJson(message); 
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        try(DatagramSocket sock = new DatagramSocket()){
            for(Map.Entry<String, SocketAddress> entry : udpClients.entrySet()){
                try{
                    DatagramPacket packet = new DatagramPacket(data, data.length, entry.getValue());
                    sock.send(packet); 
                }
                catch(IOException e){
                    System.err.println("Errore invio UDP a " + entry.getKey() + ": " + e.getMessage());
                }
            }   
        }
        catch(SocketException e){
            System.err.println("Errore creazione socket UDP: " + e.getMessage());
        }
    }

    public void notifyOne(SocketAddress address, Object message){
        String json = JsonUtils.GSON.toJson(message);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        try(DatagramSocket sock = new DatagramSocket()){
            DatagramPacket packet = new DatagramPacket(data, data.length, address);
            sock.send(packet);
        }
        catch(SocketException e){
            System.err.println("Errore creazione socket UDP: " + e.getMessage());
        }
        catch(IOException e){
            System.err.println("Errore invio UDP: " + e.getMessage());
        }
    }
}
