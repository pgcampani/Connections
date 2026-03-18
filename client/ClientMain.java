package client; 

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.Scanner;

public class ClientMain{
    
    private static int server_tcp_port; 
    private static String server_host;  
    private static boolean is_logged = false; 
    private static DatagramSocket activeUdpSocket = null; 
    public static void main(String[] args){
        readConfig("config/ClientConfig.properties");

        // Apro canale TCP verso il server - chiuso automaticamente dal try-with-resources
        try(SocketChannel socketChannel = SocketChannel.open()){
            socketChannel.configureBlocking(true);  // Modalità bloccante 
            socketChannel.connect(new InetSocketAddress(server_host, server_tcp_port)); 

            // Buffer per invio e ricezione
            ByteBuffer write_buffer = ByteBuffer.allocate(1024); 
            ByteBuffer read_buffer = ByteBuffer.allocate(1024); 
            
            Scanner scanner = new Scanner(System.in); 

            // Comandi iniziali disponibili
            System.out.println("Comandi disponibili:");
            System.out.println(">  register");
            System.out.println(">  login");
            System.out.println(">  exit");

            // Ciclo principale - gestione comandi utente
            while(true){ 
                System.out.print(">  "); 
                if(!scanner.hasNextLine()){
                    System.out.println("Connessione chiusa.");
                    break; 
                }
                String input = scanner.nextLine().trim(); 

                // Parsing dei comandi
                if(input.isEmpty()) continue; 
                
                // Comandi utente non loggato
                if(!is_logged){
                    switch(input){
                        case "register":
                            ClientCommands.handleRegister(scanner, socketChannel, write_buffer, read_buffer);
                            break;
                        
                        case "login": 
                            DatagramSocket socket = ClientCommands.handleLogin(scanner, socketChannel, write_buffer, read_buffer);
                            
                            if(socket != null){
                                activeUdpSocket = socket; 
                                is_logged = true;
                                // Avvio thread UDPListener
                                Thread udpThread = new Thread(new UDPListener(activeUdpSocket));
                                udpThread.setDaemon(true);
                                udpThread.start();
                            }
                            break; 
                        
                        case "update credential":
                            ClientCommands.updateCredential(scanner, socketChannel, write_buffer, read_buffer);
                            break; 
                        
                        case "exit":
                            System.out.println("Disconnessione...");
                            return;
                        
                        case "help":
                            System.out.println("Comandi disponibili:");
                            System.out.println(">  register");
                            System.out.println(">  login");
                            System.out.println(">  exit");
                            break;

                        default:
                            System.out.println("Comando non valido");
                            break; 
                    }
                }
                else{
                    // Comandi utente loggato
                    switch(input){
                        case "register":
                            System.out.println("Utente già registrato"); 
                            break;
                        
                        case "login":   
                            System.out.println("Utente già loggato");
                            break;

                        case "logout": 
                            if(ClientCommands.handleLogout(socketChannel, write_buffer, read_buffer)){
                                is_logged = false;
                                if(activeUdpSocket != null){
                                    activeUdpSocket.close();
                                    activeUdpSocket = null; 
                                } 
                            } 
                            break;
                        
                        case "update credential":
                            ClientCommands.updateCredential(scanner, socketChannel, write_buffer, read_buffer);
                            break; 

                        case "submit proposal":
                            ClientCommands.submitProposal(scanner, socketChannel, write_buffer, read_buffer); 
                            break; 

                        case "request game info": 
                            ClientCommands.handleGameInfo(scanner, socketChannel, write_buffer, read_buffer);
                            break; 

                        case "game stats":
                            ClientCommands.handleGameStats(scanner, socketChannel, write_buffer, read_buffer); 
                            break; 
                        
                        case "leaderboard":
                            ClientCommands.handleLeaderboard(scanner, socketChannel, write_buffer, read_buffer);
                            break; 
                        
                        case "player stats":
                            ClientCommands.handlePlayerStats(socketChannel, write_buffer, read_buffer); 
                            break;
                        
                        case "exit":
                            ClientCommands.handleLogout(socketChannel, write_buffer, read_buffer); 
                            if(activeUdpSocket != null) activeUdpSocket.close(); 
                            System.out.println("Disconnessione..."); 
                            return; 

                        case "help":
                            System.out.println("Comandi disponibili:");
                            System.out.println(">  register");
                            System.out.println(">  login");
                            System.out.println(">  update credential");
                            System.out.println(">  submit proposal");
                            System.out.println(">  request game info"); 
                            System.out.println(">  game stats");
                            System.out.println(">  player stats"); 
                            System.out.println(">  leaderboard"); 
                            System.out.println(">  logout");
                            System.out.println(">  exit");
                            break;
                        
                        default: 
                            System.out.println("Comando non valido"); 
                            break; 
                    }
                }
            }
        }
        catch(IOException e){
            System.err.println("Errore connessione: " + e.getMessage());
            System.exit(0); 
        }
    }

    public static void readConfig(String path){

        Properties prop = new Properties(); 

        try(InputStream input = new FileInputStream(path)){
            prop.load(input); 
            server_host = prop.getProperty("server.host");
            server_tcp_port = Integer.parseInt(prop.getProperty("server.tcp.port"));
        }    
        catch(IOException ex){
            System.err.println("[CLIENT]: Errore lettura file di configurazione"); 
            ex.printStackTrace();
            System.exit(1);
        }        
    }   
}
