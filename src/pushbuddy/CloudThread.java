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
			//Check if there is a connection to the cloud
            System.out.println("Pinging cloud service...");
            cloud.verifyIfUp();
			
			//Push local changes up
            System.out.println("Syncing local changes...");
            cloud.syncLocalChanges();
			
			//Pull remote changes down
            System.out.println("Syncing remote changes...");
            cloud.syncRemoteChanges();
			
			//Blocks and waits for any changes that happen on remote or local
            System.out.println("Waiting for changes...");
            cloud.waitForChanges();
        }
    }
    
    /**
     * @return the Cloud associated with this CloudThread 
     */
    public Cloud getCloud(){
        return cloud;
    }
}
