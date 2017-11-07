package pushbuddy;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Maps relationships between cloud files and local files.
 * @author Brandon Wong
 * @author Eyal Kalderon
 */
public class Tags {
    private File tagFile;
    private WatchKey tagKey;
    private HashMap<String, Path> tags; //Cloud then Local
    private ArrayList<WatchKey> watched;
    private WatchService fileWatcher;

    /**
     * Initializes a new tag database.
     * @param tagFilePath path to the tag database file
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

	public File getTagFile(){
		return tagFile;
	}

    /**
     * Called when WatchKey of database file is modified
     * Parse the tag file and rebuild the database in memory.
     */
    public void rebuildData() {
		clear();
        try (BufferedReader br = new BufferedReader(new FileReader(tagFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                //String cloudPath = line.split(";")[0];
                String localPath = line.split(";")[1];
                add(Paths.get(localPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //printWatchedDirs();
        //printContents();

    }
    
    /**
     * Tags a local file or directory to be watched for sync-ing to the cloud.
     * If it is a directory, it is traversed recursively.
     * @param local location on the local file system
     */
    public void add(Path local) {//TODO modify for duplicate names
        //System.out.println("Adding Local File: "+local);
        File target = local.toFile();
        
        if (!target.exists()) {
            return;
        }
		
        try {
            if (local.toFile().isFile()) {
                Path localRelative = local.getParent().relativize(local);
                String cloud = ("/" + localRelative).replace("\\", "/");
                cloud = resolveRemoteDupl(cloud);
                tags.put(cloud, local);
                watched.add(local.getParent().register(fileWatcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
                return;
            }
            //Else if it is a directory:
            watched.add(local.register(fileWatcher, ENTRY_CREATE, ENTRY_DELETE,
                                       ENTRY_MODIFY));
            //System.out.println("\tWalking Through Local File: "+local);
            Files.walk(local)//Walks through all the files recursively
                 .forEach(sub -> {
                     if (sub.toFile().isDirectory()) { //Add a watcher for each directory
                         try {
                             watched.add(sub.register(fileWatcher, ENTRY_CREATE,
                                                      ENTRY_DELETE,
                                                      ENTRY_MODIFY));
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     } else { //If we stumble upon a file add it
                         Path localRelative = local.getParent().relativize(sub);
                         String cloud = ("/" + localRelative).replace("\\", "/");
                         cloud = resolveRemoteDuplDirectory(local.toString(), cloud);
                         tags.put(cloud, sub);
                     }
                 });
        } catch (IOException e) {
            System.err.println(local + " could not be accessed!");
        }
    }
    /**
     * Checks if there is a folder on cloud with the same name as param folder, but are actually different folders on the local machine
     * @param folder The Path of the folder tagged
     * @return 
     */
    public boolean isDuplRemoteDirectory(String folder){
        //String selectedFolderName = '/'+folder.getParent().relativize(folder).toString();
        String selectedFolderName = folder.substring(folder.lastIndexOf(File.separator));
        if(PushBuddy.os.equals("Windows"))
            selectedFolderName = selectedFolderName.replace('\\','/');
        //System.out.println("selectedFolderName "+selectedFolderName);
        for (Map.Entry<String, Path> e : tags.entrySet()) {
            //System.out.println("\t"+e.getKey().substring(0,selectedFolderName.length())+"\tOriginal: "+e.getKey()+"\tValue: "+e.getValue());
            if(selectedFolderName.equals(e.getKey().substring(0,selectedFolderName.length()))&&numMatches(e.getKey(),"/")!=1){//Root cloud paths are equal - Potential duplicate
                if(PushBuddy.os.equals("Windows"))
                       selectedFolderName = selectedFolderName.replace('/','\\');
                String valuePath = e.getValue().toString();
                int cutoff = valuePath.indexOf(selectedFolderName)+selectedFolderName.length();
                //System.out.println("\t\t"+folder);
                if(!valuePath.substring(0,cutoff).equals(folder))//Here on the local machine, they have the same folder name, but are different folders
                    return true;
            }        
        }
        return false;
    }
    /**
     * Returns the number of times matcher appears in str
     * @param str
     * @param matcher
     * @return 
     */
    private int numMatches(String str, String matcher){
        int count=0;
        int index=0;
        while ((index = str.indexOf(matcher, index)) != -1) {
            count++;
            index += matcher.length();
        }
        return count;
    }
    public String resolveRemoteDuplDirectory(String folder, String remotePath){
		System.out.println(folder);
        if(isDuplRemoteDirectory(folder)){
			System.out.println("YES");
            //String selectedFolderName = '/'+folder.getParent().relativize(folder).toString();
            String selectedFolderName = folder.substring(folder.lastIndexOf(File.separator));
            if(PushBuddy.os.equals("Windows"))
                selectedFolderName = selectedFolderName.replace('\\','/');
            String before = remotePath.substring(0, selectedFolderName.length())+"(1)";
            String after = remotePath.substring(selectedFolderName.length());
            remotePath = before+after;
            folder+="(1)";
            //rebuild Path folder
        }
        //System.out.println(folder);
        //System.out.print(remotePath+" -> ");
        for(int i=2;isDuplRemoteDirectory(folder);i++){
            String before = remotePath.substring(0,remotePath.indexOf('/', 1));
            before = new StringBuilder(before).reverse().toString().replaceFirst("\\)\\d+\\(",")"+i+"(");
            before = new StringBuilder(before).reverse().toString();
            String after = remotePath.substring(remotePath.indexOf('/',1));
            remotePath = before+after;
            folder = new StringBuilder(folder).reverse().toString().replaceFirst("\\)\\d+\\(",")"+i+"(");
            folder = new StringBuilder(folder).reverse().toString();            
        }           
        return remotePath;
    }
    
    /**
     * Get a local path from a cloud service's path.
     * @param remote the remote file/folder location
     * @return the file/folder location on the local machine, otherwise null
     */
    public Path getLocalPath(String remote) {
        return tags.get(remote);
    }
    
    /**
     * Get a remote cloud-hosted path from a local path.
     * @param local the local file/folder location
     * @return the file/folder location on the cloud, otherwise null
     */
    public String getRemotePath(Path local) {
        for (Map.Entry<String, Path> e : tags.entrySet()) {
            if (e.getValue().equals(local)) {
                return e.getKey();
            }
        }
        return null;
    }
    
    /**
     * Checks whether any tagged local files changed on the filesystem.
     * @return true there was a change, false otherwise
     */
    public boolean localFilesChanged() {
        return fileWatcher.poll() != null;
    }
    
    /**
     * Checks whether the local tag database has been modified.
     * @return true if the file was modified, false otherwise
     */
    public boolean tagFileChanged() {
        for (WatchEvent<?> event : tagKey.pollEvents()) {
            WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
            WatchEvent.Kind<?> kind = pathEvent.kind();
            
            return kind == ENTRY_MODIFY;
        }
        
        return false;
    }
    
    /**
     * Retrieves a set of WatchKeys of watched local directories.
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
     * Retrieves a list of tagged files on the local system.
     * @return an array of local file paths
     */
    public Path[] getLocalFiles(){
        Collection<Path> local = tags.values();
        return local.toArray(new Path[local.size()]);
    }
    
    /**
     * Retrieves a list of tagged files hosted remotely in the cloud.
     * @return an array of remote file paths
     */
    public String[] getRemoteFiles(){
        Set<String> remote = tags.keySet();
        return remote.toArray(new String[remote.size()]);
    }
    
    public HashMap<String, Path> getTags(){
        return tags;
    }
    /**
     * Prints the contents of the database to stdout for debugging purposes.
     */
    public void printContents() {
        for (Map.Entry<String, Path> e : tags.entrySet()) {
            System.out.println(e.getKey() + ";" + e.getValue().toString());
        }
    }
    
    /**
     * Checks for Duplicate remote paths in this tag database
     * @param remotePath the remote path to check against
     * @return true if this remote path is already in the tag database
     */
    public boolean isDuplRemote(String remotePath){
        return tags.containsKey(remotePath);
    }
    
    /**
     * Resolves Duplicate names in the remote file path if it needs to be resolved. Otherwise, no change
     * Follows the renaming scheme: file, file(1), file(2), ... , file(n)
     * @param remotePath the remote path to resolve
     * @return the renamed remote file path
     */
    public String resolveRemoteDupl(String remotePath){
        if(isDuplRemote(remotePath)){//Check for the first duplicate copy
            int extensionIndex = remotePath.indexOf('.');//TODO this doesnt work for things named like ".gitignore"
            if(extensionIndex>=0){
                String newRemotePath = remotePath.substring(0,extensionIndex)+"(1)";
                String extension = remotePath.substring(extensionIndex);
                remotePath = newRemotePath + extension;
            }else{
                remotePath+="(1)";
            }
        }
        //System.err.println(remotePath);
        for(int i=2;isDuplRemote(remotePath);i++){ //Keep checking if there is more than 2 duplicates 
            int extensionIndex = remotePath.indexOf('.');//TODO this doesnt work for things named like ".gitignore"
            if(extensionIndex>=0){
                String newRemotePath = remotePath.substring(0,extensionIndex);
                newRemotePath = new StringBuilder(newRemotePath).reverse().toString().replaceFirst("\\)\\d+\\(",")"+i+"(");
                newRemotePath = new StringBuilder(newRemotePath).reverse().toString();
                String extension = remotePath.substring(extensionIndex);
                remotePath = newRemotePath + extension;

            }else{
                remotePath = new StringBuilder(remotePath).reverse().toString().replaceFirst("\\)\\d+\\(",")"+i+"(");
                remotePath = new StringBuilder(remotePath).reverse().toString();
            }    
        }
        //printContents();
        //System.out.println("NEW NAME (IN METHOD): "+remotePath);
        return remotePath;
    }
    

    public void printWatchedDirs(){
        for(WatchKey k:watched){
            System.out.println("Watched Dirs:");
            System.out.println("\t"+k.toString());
        }
    }
}
