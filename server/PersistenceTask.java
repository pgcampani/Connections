package server;

import server.game.GameManager;

public class PersistenceTask implements Runnable{
    public final UserManager userManager;
    private final GameManager gameManager; 
    private final String stateFile; 

    public PersistenceTask(UserManager userManager, GameManager gameManager, String stateFile){
        this.userManager = userManager; 
        this.gameManager = gameManager;
        this.stateFile = stateFile; 
    }

    @Override
    public void run(){
        userManager.saveUsers();
        gameManager.saveState(stateFile); 
    }
}
