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
        
        while (running) {
            cloud.pingService();
            cloud.syncLocalChanges();
            cloud.syncRemoteChanges();
            cloud.waitForChanges();
        }
    }
}
