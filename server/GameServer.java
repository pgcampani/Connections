package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private final ServerConfig config;
    private ExecutorService threadPool;

    public GameServer(ServerConfig config){
        this.config = config;  
    }

    public void start(){
        int tcp_port = config.getTcpPort();
        int threadPoolSize = config.getPoolSize();
    
        threadPool = Executors.newFixedThreadPool(threadPoolSize);

        try(ServerSocket serverSocket = new ServerSocket(tcp_port)){
            while(true){
                Socket clientSocket = serverSocket.accept();    // bloccante
                threadPool.execute(new ClientHandler(clientSocket));

                System.out.println("Client " + clientSocket + " connesso"); 
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        finally{
            threadPool.shutdown(); 
        }
    }
}
