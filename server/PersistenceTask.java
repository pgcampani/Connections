package server;

public class PersistenceTask implements Runnable{
    public final UserManager userManager; 

    public PersistenceTask(UserManager userManager){
        this.userManager = userManager; 
    }

    @Override
    public void run(){
        userManager.saveUsers();
    }
}
