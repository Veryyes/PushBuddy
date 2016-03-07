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
            cloud.syncLocalChanges();
            cloud.syncRemoteChanges();
            cloud.waitForChanges();
        }
    }
}
