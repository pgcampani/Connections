package server; 

import com.google.gson.JsonParser; 

import messages.JsonUtils; 
import messages.NetworkUtils; 
import messages.responses.RegisterResponse; 
import messages.requests.RegisterMessage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.NetworkChannel;

public class ClientHandler implements Runnable{

    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run(){
        try(Socket socket = clientSocket;
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8")), true);
        ){
            String message;
            System.out.println("[CLIENT HANDLER]: Siamo nel client handler"); 
            
            while((message = NetworkUtils.TCPreceive(in)) != null){
                String operation = JsonParser.parseString(message).getAsJsonObject().get("operation").getAsString(); 
                switch(operation){
                    case "register": 
                        handleRegister(message, out); 
                        break; 
                    
                    default: 
                        NetworkUtils.TCPsend(out, new RegisterResponse("ERROR", "Operazione non riconosciuta")); 
                }
            }

            System.out.println("Client disconnesso."); 
        }
        catch(SocketException se){
            System.out.println("Client disconnesso."); 
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void handleRegister(String message, PrintWriter out){
        RegisterMessage request = JsonUtils.GSON.fromJson(message, RegisterMessage.class);

        System.out.println("Registrazione: " + request.name); 

        NetworkUtils.TCPsend(out, new RegisterResponse("OK", "Registrazione avvenuta con successo!")); 
    }
}
