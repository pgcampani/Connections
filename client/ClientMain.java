package client; 

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.Scanner;

public class ClientMain{
    
    private static int server_tcp_port; 
    private static int server_udp_port;
    private static String server_host;  
    private static boolean is_registered = false; 
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
                    switch(input){
                        case "register":
                            is_registered = ClientCommands.handleRegister(scanner, socketChannel, write_buffer, read_buffer);
                            break;
                        
                        case "login": 
                            is_registered = ClientCommands.handleLogin(scanner, socketChannel, write_buffer, read_buffer);
                            break; 
                        
                        case "exit":
                            System.out.println("Disconnessione...");
                            return;

                        default:
                            System.out.println("Comando non valido");
                            break; 
                    }
                }
                else{
                    switch(input){
                        case "register":
                            System.out.println("Utente già registrato"); 
                            break;
                        
                        case "login":   
                            System.out.println("Utente già loggato");
                            break;

                        case "logout": 
                            if(ClientCommands.handleLogout(scanner, socketChannel, write_buffer, read_buffer)){
                                is_registered = false; 
                            } 
                            break;
                        
                        case "exit":
                            System.out.println("Disconnessione..."); 
                            return; 
                        
                        default: 
                            System.out.println("Comando non valido"); 
                            break; 
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
