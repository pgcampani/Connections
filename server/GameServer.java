package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import server.game.GameManager;

public class GameServer{
    private final ServerConfig config;
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduler; 

    public GameServer(ServerConfig config){
        this.config = config;  
    }

    public void start(){
        int tcp_port = config.getTcpPort();
        int threadPoolSize = config.getPoolSize();
        int persistenceInterval = config.getPersistenceInterval(); 
        int gameDuration = config.getGameDuration(); 
        UserManager userManager = new UserManager(config.getUsersFile()); 

        GameManager gameManager;
        try {
            gameManager = new GameManager(userManager, config.getConnectionsData(),gameDuration);
            gameManager.start();
        }
        catch(IOException e){
            System.err.println("Errore apertura file " + e.getMessage());
            return; 
        } 

        threadPool = Executors.newFixedThreadPool(threadPoolSize);

        scheduler = Executors.newScheduledThreadPool(1); 
        scheduler.scheduleAtFixedRate(new PersistenceTask(userManager), persistenceInterval, persistenceInterval, TimeUnit.SECONDS); 

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            @Override
            public void run(){
                userManager.logoutAll();
                userManager.saveUsers();
                threadPool.shutdown();
                scheduler.shutdown();
                gameManager.stop(); 
                System.out.println("Chiusura server terminata");
            }
        }));

        try(ServerSocket serverSocket = new ServerSocket(tcp_port)){
            while(true){
                Socket clientSocket = serverSocket.accept();    // bloccante
                threadPool.execute(new ClientHandler(clientSocket, userManager, gameManager));

                System.out.println("Client " + clientSocket + " connesso"); 
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        finally{
            if(!threadPool.isShutdown()) threadPool.shutdown(); 
            if(!scheduler.isShutdown()) scheduler.shutdown();
        }
    }
}