package server; 

import com.google.gson.JsonParser; 

import messages.JsonUtils; 
import messages.NetworkUtils; 
import messages.responses.*; 
import messages.requests.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;


public class ClientHandler implements Runnable{

    private final Socket clientSocket;
    private final UserManager userManager; 

    public ClientHandler(Socket socket, UserManager userManager){
        this.clientSocket = socket;
        this.userManager = userManager; 
    }   

    @Override
    public void run(){

        String loggedUsername = null; 

        try(Socket socket = clientSocket;
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)), true);
        ){
            String message;

            while((message = NetworkUtils.TCPreceive(in)) != null){

                String operation = JsonParser.parseString(message).getAsJsonObject().get("operation").getAsString(); 

                switch(operation){
                    case "register": 
                        loggedUsername = handleRegister(message, out); 
                        break; 
                    
                    case "login":
                        loggedUsername = handleLogin(message, out); 
                        break; 
                    
                    case "logout": 
                        handleLogout(loggedUsername, out); 
                        loggedUsername = null; 
                        break; 
                    
                    default: 
                        NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Operazione non riconosciuta")); 
                        break; 
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
        finally{
            if(loggedUsername != null){
                userManager.logout(loggedUsername); 
                System.out.println("Logout automatico effettuato per " + loggedUsername); 
            }
        }
    }

    private String handleRegister(String message, PrintWriter out){
        RegisterMessage request = JsonUtils.GSON.fromJson(message, RegisterMessage.class);

        if(userManager.register(request.name.trim(), request.password.trim())){
            userManager.login(request.name.trim(), request.password.trim()); 
            NetworkUtils.TCPsend(out, new ServerResponse("OK", "Registrazione avvenuta con successo"));
            return request.name.trim(); 
        }
        else{
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Username occupato"));
            return null; 
        }
    }

    private String handleLogin(String message, PrintWriter out){
        LoginMessage request = JsonUtils.GSON.fromJson(message, LoginMessage.class);
        String result = userManager.login(request.username.trim(), request.password.trim()); 

       switch(result){
        case "OK": 
            NetworkUtils.TCPsend(out, new ServerResponse("OK", "Login effettuato con successo"));
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
       }

       return null; 
    }

    private void handleLogout(String loggedUsername, PrintWriter out){
        if(loggedUsername == null){
            NetworkUtils.TCPsend(out, new ServerResponse("ERROR", "Nessun utente loggato"));
            return; 
        }
        userManager.logout(loggedUsername); 
        NetworkUtils.TCPsend(out, new ServerResponse("OK", "Logout avvenuto con successo"));
    }
}
