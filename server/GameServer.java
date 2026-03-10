package server;

import java.io.FileReader;
import java.io.IOException;
import java.io.File; 
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import server.game.GameManager;
import server.game.GameStats;
import messages.JsonUtils;
import server.game.GameManagerState;

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
        
        int nextGameIndex = 0;
        ConcurrentHashMap<Integer, GameStats> gameStats = null; 
        File stateFile = new File(config.getGameStateFile());

        if(stateFile.exists()){
            try(FileReader reader = new FileReader(stateFile)){
                GameManagerState state = JsonUtils.GSON.fromJson(reader, GameManagerState.class);
                if(state != null){
                    nextGameIndex = state.nextGameIndex; 
                    gameStats = state.gameStats; 
                }
            }
            catch(IOException e){
                    e.printStackTrace();
            }
        }

        GameManager gameManager;
        try {
            gameManager = new GameManager(userManager, config.getConnectionsData(), gameDuration, nextGameIndex, gameStats, config.getGameStateFile());
            gameManager.start();
        }
        catch(IOException e){
            System.err.println("Errore apertura file " + e.getMessage());
            return; 
        } 

        threadPool = Executors.newFixedThreadPool(threadPoolSize);

        scheduler = Executors.newScheduledThreadPool(1); 
        scheduler.scheduleAtFixedRate(new PersistenceTask(userManager,gameManager, config.getGameStateFile()), persistenceInterval, persistenceInterval, TimeUnit.SECONDS); 
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            @Override
            public void run(){
                threadPool.shutdown();
                scheduler.shutdown();
                gameManager.stop(); 
                try{
                    if(!threadPool.awaitTermination(10, TimeUnit.SECONDS)) threadPool.shutdownNow();
                    if(!scheduler.awaitTermination(5, TimeUnit.SECONDS)) scheduler.shutdownNow();
                } catch(InterruptedException e){
                    threadPool.shutdownNow();
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                gameManager.endGame(); 
                userManager.logoutAll();
                userManager.saveUsers();
                gameManager.saveState(config.getGameStateFile()); // salva stato finale
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