package pushbuddy;

/**
 * Synchronizes a cloud service with the local machine.
 * 
 * @author Brandon Wong
 * @author Eyal Kalderon
 */
public class CloudThread extends Thread {
    private Cloud cloud;
    public static volatile boolean running = true;
    
    /**
     * Initializes a new thread with the given cloud service.
     * @param serviceToUse the cloud service to sync from
     */
    CloudThread(Cloud serviceToUse) {
        cloud = serviceToUse;
    }
    
    /**
     * Executes the service on a new thread.
     */
    public void run() {
        cloud.authenticate();
        
        while (running) {
            cloud.verifyIfUp();
            cloud.syncLocalChanges();
            cloud.syncRemoteChanges();
            cloud.waitForChanges();
        }
    }
}
