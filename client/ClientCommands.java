package client; 

import messages.JsonUtils; 
import messages.NetworkUtils; 
import messages.requests.*; 
import messages.responses.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClientCommands{
    public static boolean handleRegister(Scanner scanner, SocketChannel socketChannel, ByteBuffer write_b, ByteBuffer read_b) throws IOException{
        System.out.print("Username ");
        String username = scanner.nextLine(); 
        System.out.print("Password "); 
        String password = scanner.nextLine(); 

        NetworkUtils.NIOsend(socketChannel, write_b, new RegisterMessage(username, password));
        String raw = NetworkUtils.NIOreceive(socketChannel, read_b); 
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);

        if(response.status.equals("OK")){
            System.out.println("Utente registrato " + response.message);
            return true; 
        }
        else{
            System.out.println("Errore: " + response.message);
            return false; 
        } 
    }

    public static boolean handleLogin(Scanner scanner, SocketChannel socketChannel, ByteBuffer write_b, ByteBuffer read_b) throws IOException{
        System.out.print("Username ");
        String username = scanner.nextLine(); 
        System.out.print("Password "); 
        String password = scanner.nextLine();  

        NetworkUtils.NIOsend(socketChannel, write_b, new LoginMessage(username, password));
        String raw = NetworkUtils.NIOreceive(socketChannel, read_b);
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);

        if(response.status.equals("OK")){
            System.out.println("Login success! " + response.message);
            return true;
        }
        else{
            System.out.println("Errore: " + response.message);
            return false;
        }
    }

    public static boolean handleLogout(Scanner scanner, SocketChannel socketChannel, ByteBuffer write_b, ByteBuffer read_b) throws IOException{
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
}
