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
        String username = scanner.nextLine().trim(); 
        System.out.print("Password "); 
        String password = scanner.nextLine().trim();
        
        if(username.isEmpty() || password.isEmpty()){
            System.out.println("Errore: i campi Username e Password non possono essere vuoti");
            return false; 
        }

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
            System.out.println("Login success! " + response.message);
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

        NetworkUtils.NIOsend(socketChannel, read_b, new UpdateCredentialMessage(old_username, old_password, new_username, new_password));

        String raw = NetworkUtils.NIOreceive(socketChannel, read_b); 
        ServerResponse response = JsonUtils.GSON.fromJson(raw, ServerResponse.class);

        if(response.status.equals("OK")){
            System.out.println("Credenziali aggiornate " + response.message);
        }
        else{
            System.out.println("Errore " + response.message);
        }
    }
}
