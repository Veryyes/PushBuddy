package pushbuddy;

import java.io.File;

/**
 * Abstract API for cloud services.
 * 
 * @author Eyal Kalderon
 */
public abstract class Cloud {
    protected Tags tags;
    protected File authFile;
    
    public Cloud(String tagFilePath, String authFilePath) {
        authFile = new File(authFilePath);
        tags = new Tags(tagFilePath);
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
     * Pings the cloud service until program gets a response
     */
    public abstract void pingService();
}