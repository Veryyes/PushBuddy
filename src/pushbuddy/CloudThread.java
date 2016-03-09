package pushbuddy;

/**
 * Synchronizes changes from another thread.
 * 
 * @author Brandon
 */
public class CloudThread extends Thread {
    private Cloud cloud;
    public static volatile boolean running = true;
    
    CloudThread(Cloud serviceToUse) {
        cloud = serviceToUse;
    }
    
    public void run() {
        cloud.authenticate();
        cloud.readTagFile();
        
        while (running) {
            System.out.println("Syncing Dropbox...");
            //Ping Respective Cloud Service
            cloud.pingService();
            cloud.syncLocalChanges();
            cloud.syncRemoteChanges();
            cloud.waitForChanges(); //Program waits at this method to look for changes
        }
    }
}
