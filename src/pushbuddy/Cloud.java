package pushbuddy;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Abstract API for cloud services.
 * 
 * @author Eyal Kalderon
 */
public abstract class Cloud {
    protected final String domain;
    protected File authFile;
    protected Tags tags;
    
    /**
     * Initializes a new cloud service.
     * 
     * It looks for two files in the same directory as the binary:
     * 1. name + "Access.txt" - File containing the needed OAuth2 key.
     * 2. name + "Tags.txt"   - Database containing tagged file data.
     * 
     * @param name name of the service
     * @param domain the domain of the cloud service, e.g. www.dropbox.com
     */
    public Cloud(String name, String domain) {
        authFile = new File(name + "Access.txt");
        tags = new Tags(name + "Tags.txt");
        this.domain = domain;
    }
    
    /**
     * Attempts to authenticate with the cloud API server(s).
     */
    public abstract void authenticate();
    
    /**
     * Iterate over all tagged local files and apply changes.
     */
    public abstract void syncLocalChanges();
    
    /**
     * Iterate over all tagged remote files and apply changes.
     */
    public abstract void syncRemoteChanges();
    
    /**
     * Block the current thread and don't return until changes are
     * detected.
     */
    public abstract void waitForChanges();
    
    /**
     * Pings the given cloud server indefinitely until it gets a response.
     */
    public void verifyIfUp() {
        boolean canReach = false;
        
        while (!canReach) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(domain, 80), 2000);
                canReach = true;
            } catch(IOException e){
                canReach = false;
            }
        }
    }
}