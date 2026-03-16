package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig{
    private int tcp_port;
    private int poolSize; 
    private int gameDuration; 
    private String connectionsData;
    private String usersFile;  
    private int persistenceInterval; 
    private String gameStateFile; 

    public static ServerConfig load(String path){
        ServerConfig config = new ServerConfig(); 
        Properties props = new Properties(); 

        try(FileInputStream fis = new FileInputStream(path)){
            props.load(fis); 

            config.tcp_port = Integer.parseInt(props.getProperty("server.tcp.port"));
            config.poolSize = Integer.parseInt(props.getProperty("server.threadpool.size"));
            config.gameDuration = Integer.parseInt(props.getProperty("server.game.duration"));
            config.usersFile = props.getProperty("server.users.file"); 
            config.persistenceInterval = Integer.parseInt(props.getProperty("server.persistence.interval")); 
            config.connectionsData = props.getProperty("server.words.file"); 
            config.gameStateFile = props.getProperty("server.game.state.file"); 
        }
        catch(IOException e){
            System.err.println("Errore lettura configurazione: " + e.getMessage());
            System.exit(1);
        }   
        
        return config; 
    }

    public int getTcpPort(){
        return tcp_port;
    }

    public int getPoolSize(){
        return poolSize;
    }

    public int getGameDuration(){
        return gameDuration;
    }

    public String getConnectionsData(){
        return connectionsData;
    }

    public String getUsersFile(){
        return usersFile; 
    }

    public int getPersistenceInterval(){
        return persistenceInterval;
    }

    public String getGameStateFile(){
        return gameStateFile; 
    }
}

