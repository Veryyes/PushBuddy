package pushbuddy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Abstract API for cloud services.
 * 
 * @author Eyal Kalderon
 */
public abstract class Cloud {
    protected Tags tags;
    
    protected File authFile;
    protected File tagFile;
    
    public Cloud(String tagPath, String authFilePath) {
        try {
            authFile = new File(authFilePath);
            tagFile = new File(tagPath);
            
            if (!tagFile.exists()) {
                tagFile.createNewFile();
            }
            
            tags = new Tags();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Attempts to authenticate with the cloud API server(s).
     */
    public abstract void authenticate();

    /**
     * Read tag data from file.
     */
    public abstract void readTagFile();

    /**
     * Write out current tag data to file.
     */
    public abstract void writeTagFile();

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
}