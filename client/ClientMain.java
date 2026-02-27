package client; 

import messages.JsonUtils;
import messages.requests.*; 
import messages.responses.*;
import messages.NetworkUtils; 

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

public class ClientMain{
    
    private static int server_tcp_port; 
    private static int server_udp_port;
    private static String server_host;  
    private static boolean is_registered = false; 
    // private static boolean is_connected = false; 

    public static void main(String[] args){
        readConfig("config/ClientConfig.properties");

        System.out.println("server.host: " + server_host);
        System.out.println("server.tcp.port: " + server_tcp_port);
        System.out.println("server.udp.port: " + server_udp_port);

        try(SocketChannel socketChannel = SocketChannel.open()){
            socketChannel.configureBlocking(true); 
            socketChannel.connect(new InetSocketAddress(server_host, server_tcp_port)); 

            ByteBuffer write_buffer = ByteBuffer.allocate(1024); 
            ByteBuffer read_buffer = ByteBuffer.allocate(1024); 
            Scanner scanner = new Scanner(System.in); 

            System.out.println("Comandi disponibili:");
            System.out.println(">  register");
            System.out.println(">  login");
            System.out.println(">  exit");

           while(true){ 
                System.out.print("> "); 
                if(!scanner.hasNextLine()){
                    System.out.println("Connessione chiusa.");
                    break; 
                }
                String input = scanner.nextLine().trim(); 

                // Parsing dei comandi
                if(input.isEmpty()) continue; 
                // REGISTRAZIONE 
                if(!is_registered){
                    if(input.equals("register")){
                        System.out.print("Username ");
                        String username = scanner.nextLine(); 
                        System.out.print("Password "); 
                        String password = scanner.nextLine(); 

                        NetworkUtils.NIOsend(socketChannel, write_buffer, new RegisterMessage(username, password));
                        String raw = NetworkUtils.NIOreceive(socketChannel, read_buffer); 
                        RegisterResponse response = JsonUtils.GSON.fromJson(raw, RegisterResponse.class);

                        if(response.status.equals("OK")){
                            is_registered = true;
                            System.out.println("Utente registrato " + response.message); 
                        }
                        else{
                            System.out.println("Errore: " + response.message);
                        }
                    }
                    else if(input.equals("login")){

                        System.out.print("Username ");
                        String username = scanner.nextLine(); 
                        System.out.print("Password "); 
                        String password = scanner.nextLine();  

                        NetworkUtils.NIOsend(socketChannel, write_buffer, new LoginMessage(username, password));
                        String raw = NetworkUtils.NIOreceive(socketChannel, read_buffer);
                        RegisterResponse response = JsonUtils.GSON.fromJson(raw, RegisterResponse.class);

                        if(response.status.equals("OK")){
                            is_registered = true; 
                            System.out.println("Login success! " + response.message);
                        }
                        else{
                            System.out.println("Errore: " + response.message);
                        }
                    }
                    else if(input.equals("exit")){
                        System.out.println("Disconnessione...");
                        break; 
                    }
                    else{
                        System.out.println("Comando non riconosciuto. Comandi disponibili:\n> register\n> login\n> exit");
                    }
                }
                else{
                    if(input.equals("register") || input.equals("login")){
                        // Ho già fatto login o registrazione -> sono pronto per giocare
                        System.out.println("Utente già connesso"); 
                    }
                    else if(input.equals("exit")){
                    System.out.println("Disconnessione...");
                    break; 
                    }

                    else if(input.equals("logout")){
                        NetworkUtils.NIOsend(socketChannel, write_buffer, new LogoutMessage());

                        String raw = NetworkUtils.NIOreceive(socketChannel, read_buffer); 
                        RegisterResponse response = JsonUtils.GSON.fromJson(raw, RegisterResponse.class); 

                        if(response.status.equals("OK")){
                            System.out.println("Logged out successfully"); 
                            is_registered = false; 
                        }
                        else{
                            System.out.println("Errore nel logout"); 
                        }

                    }
                }
            }
        }
        catch(IOException e){
            System.err.println("[CLIENT] Errore connessione: " + e.getMessage());
            System.exit(0); 
        }
    }

    public static void readConfig(String path){

        Properties prop = new Properties(); 

        try(InputStream input = new FileInputStream(path)){
            prop.load(input); 
            server_host = prop.getProperty("server.host");
            server_tcp_port = Integer.parseInt(prop.getProperty("server.tcp.port"));
            server_udp_port = Integer.parseInt(prop.getProperty("server.udp.port"));
        }    
        catch(IOException ex){
            System.err.println("[CLIENT]: Errore lettura file di configurazione"); 
            ex.printStackTrace();
            System.exit(1);
        }        
    }   
}
