package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig{
    private int tcp_port;
    private int udp_port; 
    private int poolSize; 
    private int gameDuration; 
    private String connectionsData; 

    public static ServerConfig load(String path){
        ServerConfig config = new ServerConfig(); 
        Properties props = new Properties(); 

        try(FileInputStream fis = new FileInputStream(path)){
            props.load(fis); 

            config.tcp_port = Integer.parseInt(props.getProperty("server.tcp.port"));
            config.udp_port = Integer.parseInt(props.getProperty("server.udp.port"));
            config.poolSize = Integer.parseInt(props.getProperty("server.threadpool.size"));
            config.gameDuration = Integer.parseInt(props.getProperty("server.game.duration"));
            config.connectionsData = props.getProperty("server.connections.data");
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
        return config; 
    }

    public int getTcpPort(){
        return tcp_port;
    }

    public int getUdpPort(){
        return udp_port;
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
}

