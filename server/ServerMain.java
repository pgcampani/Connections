package server; 

public class ServerMain{
    public static void main(String[] args){

        ServerConfig config = ServerConfig.load("config/ServerConfig.properties"); 
        GameServer server = new GameServer(config); 
        server.start();
    }
}