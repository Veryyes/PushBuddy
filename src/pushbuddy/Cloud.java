package pushbuddy;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Abstract API for cloud services.
 * 
 * @author Eyal Kalderon
 */
public abstract class Cloud {
    protected Tags tags;
    
    protected File authFile;
    protected String tagFilePath;
    
    protected WatchService watcher;
    protected ArrayList<WatchKey> watchedFiles;
 
    protected WatchKey tagKey;
    public Cloud(String tagFile, String authFilePath) {
        try {
            // Set up tag database file.
            tagFilePath = tagFile;
            File tagDB = new File(tagFile);
            if (!tagDB.exists()) {
                //System.out.println(System.getProperty("user.dir"));
                tagDB.createNewFile();
            }
            tags = new Tags(new File(tagFile));

            // Set up authentication file.
            authFile = new File(authFilePath);
            
            // Set up file watcher.
            watcher = FileSystems.getDefault().newWatchService();
            watchedFiles = new ArrayList<>();
            
            System.out.println(new File(tagFile).getAbsoluteFile().toPath().getParent());
            
            tagKey = new File(tagFile).getAbsoluteFile().toPath().getParent().register(watcher, ENTRY_MODIFY);
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
    
    /**
     * Uploads files in uploadList which could not be uploaded earlier due to connection issues
     */
  //  public abstract void pushInterruptedFiles();
    /**
     * Pings the cloud service until program gets a response
     */
    public abstract void pingService();
}