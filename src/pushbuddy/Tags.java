package pushbuddy;

import com.dropbox.core.DbxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    private File tagFile;
    private WatchKey tagKey;
    private HashMap<Path, Path> tags;
    private ArrayList<WatchKey> watched;
    private WatchService fileWatcher;

    /**
     * Creates a new tag database.
     */
    public Tags(String tagFilePath) {
        tagFile = new File(tagFilePath);
        tags = new HashMap<>();
        watched = new ArrayList<>();
        
        try {
            if (!tagFile.exists()) {
                tagFile.createNewFile();
            }
            
            fileWatcher = FileSystems.getDefault().newWatchService();
            tagKey = Paths.get(tagFile.getAbsolutePath()).getParent().register(fileWatcher, ENTRY_MODIFY);
        } catch (IOException e) {
            e.printStackTrace();
        }
        rebuildData();
    }
    /**
     * 
     */
    public void rebuildData(){        
        // Parse the tag file and watch our local data.
        try (BufferedReader br = new BufferedReader(new FileReader(tagFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Load tags.
                // cloudPath not being used
                String cloudPath = line.split(";")[0];
                String localPath = line.split(";")[1];
                add(Paths.get(localPath));
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        printContents();
    }
    
    /**
     * Adds new tag(s) to the database. If it is a directory, it is traversed
     * recursively.
     * 
     * @param local A location on the local file system.
     */
    public void add(Path local) {
        if (local.toFile().isFile()) {
            Path localRelative = local.getParent().relativize(local);
            Path cloud = Paths.get("/" + localRelative.toString());
            tags.put(cloud, local);
            return;
        }
       
        try {
            watched.add(local.register(fileWatcher, ENTRY_CREATE,
                                               ENTRY_DELETE,
                                               ENTRY_MODIFY));
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
    public String getRemotePath(Path local) {
        for (Map.Entry<Path, Path> e : tags.entrySet()) {
            if (e.getValue().equals(local)) {
                return e.getKey().toString().replace("\\", "/");
            }
        }
        return "";
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
     * 
     * @return true if tagFile is modified
     */
    public boolean tagFileChanged(){
        for(WatchEvent<?> event: tagKey.pollEvents()){
            WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
            WatchEvent.Kind<?> kind = pathEvent.kind();
            
            return kind == ENTRY_MODIFY;
        }
        return false;
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
     * 
     * @return 
     */
    public Path[] getLocalFiles(){
        Path[] p = new Path[tags.values().size()];
        return tags.values().toArray(p);
    }
    
    public Path[] getRemoteFiles(){
        Path[] p = new Path[tags.keySet().size()];
        return tags.keySet().toArray(p);
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
