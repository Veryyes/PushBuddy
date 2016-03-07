package pushbuddy;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A set of tagged files loaded from a file.
 * File format: /location/on/cloud;/location/on/local/machine\n
 *
 * @author Eyal Kalderon
 */
public class Tags {
    private HashMap<Path, Path> tags;
    private ArrayList<WatchKey> watched;
    private WatchService fileWatcher;

    /**
     * Creates a new tag database.
     */
    public Tags() {
        tags = new HashMap<>();
        watched = new ArrayList<>();
        
        try {
            fileWatcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds new tag(s) to the database. If it is a directory, it is traversed
     * recursively.
     * 
     * @param remote A location on the cloud service.
     * @param local A location on the local filesystem.
     */
    public void add(Path remote, Path local) {
        File root = new File(local.toUri());

        try {
            Files.walk(local)
                 .forEach(sub -> {
                     if (sub.toFile().isDirectory()) {
                         try {
                             watched.add(sub.register(fileWatcher, ENTRY_CREATE,
                                                      ENTRY_DELETE,
                                                      ENTRY_MODIFY));
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     } else {
                         Path localRelative = local.getParent().relativize(sub);
                         Path cloud = Paths.get("/" + localRelative.toString());
                         tags.put(cloud, sub);
                     }
                 });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Get a local path from a cloud service's path.
     * 
     * @param remote The remote file/folder location.
     * @return The file/folder location on the local machine, otherwise null.
     */
    public Path getLocalPath(Path remote) {
        return tags.get(remote);
    }
    
    /**
     * Get a remote cloud-hosted path from a local path.
     * 
     * @param local The local file/folder location.
     * @return The file/folder location on the cloud server, otherwise null.
     */
    public Path getRemotePath(Path local) {
        for (Map.Entry<Path, Path> e : tags.entrySet()) {
            if (e.getValue() == local) {
                return e.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Checks whether any tagged local files changed on the filesystem.
     * 
     * @return True there was a change, false if there was not.
     */
    public boolean localFilesChanged() {
        return fileWatcher.poll() != null;
    }
    
    /**
     * Retrieves a set of WatchKeys of watched local directories.
     * 
     * @return An array list of WatchKeys.
     */
    public ArrayList<WatchKey> getWatchedDirs() {
        return watched;
    }
    
    /**
     * Clears all tagged files from the database.
     */
    public void clear() {
        for (WatchKey w : watched) {
            w.cancel();
        }
        
        watched.clear();
        tags.clear();
    }

    /**
     * Prints the contents of the database to stdout for debugging purposes.
     */
    public void printContents() {
        for (Map.Entry<Path, Path> e : tags.entrySet()) {
            System.out.println(e.getKey().toString() + ";" + e.getValue().toString());
        }
    }
}
