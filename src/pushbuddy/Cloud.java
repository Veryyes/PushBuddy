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
	protected boolean enabled;
    
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
		enabled=true;
    }
	
	public Tags getTags(){
		return tags;
	}
    
    /**
     * Attempts to authenticate with the cloud API server(s).
     */
    public abstract void authenticate();
    
    /**
     * Iterates over all tagged local files and apply changes.
     */
    public abstract void syncLocalChanges();
    
    /**
     * Iterates over all tagged remote files and apply changes.
     */
    public abstract void syncRemoteChanges();
    
    /**
     * Checks whether any remote files were changed.
     * @return true if changed, false otherwise
     */
    public abstract boolean remoteFilesChanged();
    
    /**
     * Blocks the current thread and don't return until changes are
     * detected.
     */
    public void waitForChanges() {
        boolean fileChanged = false;
        while (!fileChanged) {
            try {
                Thread.sleep(1000);
                
                // Remote changes
                if (remoteFilesChanged()) {
                    fileChanged = true;
                }
                
                //Local Changes
                if (tags.localFilesChanged()) {
                    fileChanged = true;
                } else if (tags.tagFileChanged()) {
                    tags.rebuildData();
                    fileChanged = true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.err.println("[LOCAL CHANGE ERROR]");
            }
        }
    }
    
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
    
    /**
     * Checks for Duplicate remote paths in this cloud's tag database
     * @param remotePath the remote path to check against
     * @return true if this remote path is already in the cloud's tag database
     */
    public boolean isDuplRemote(String remotePath){
        return tags.isDuplRemote(remotePath);
    }
}