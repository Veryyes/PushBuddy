/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pushbuddy;

import com.dropbox.core.*;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.HashSet;
import javax.swing.JOptionPane;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.LinkedList;

/**
 * Provides Dropbox cloud API support.
 * 
 * @author Eyal Kalderon
 */
public class Dropbox extends Cloud{
    private final static String APP_KEY = "24j4nfy5tf7v55s";
    private final static String APP_SECRET = "15gkteuuhklh5q1";
    private String authToken;
    private DbxClient client;
    private DbxDelta<DbxEntry> changes = null;
    private String returnedFiles = null;
    private HashSet<Path> localDirs;
    
    
    public Dropbox(String tagFile, String authFilePath) {
        super(tagFile, authFilePath);
        localDirs = new HashSet<>();
        
    }
    
    @Override
    public void authenticate() {
        DbxAppInfo info = new DbxAppInfo(APP_KEY, APP_SECRET);
        DbxRequestConfig config = new DbxRequestConfig("PushBuddy/1.0",
            Locale.getDefault().toString());
        
        if (authFile.exists()) {
            useExistingAuth();
            if(authToken == null)
                genNewAuth(config, info);
        } else {
            genNewAuth(config, info);
        }
        
        client = new DbxClient(config, authToken);
        
        try {
            changes = client.getDelta(returnedFiles);
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }
    
    private void useExistingAuth() {
        try (BufferedReader br = new BufferedReader(new FileReader(authFile))){
            authToken = br.readLine();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void genNewAuth(DbxRequestConfig config, DbxAppInfo info) {
        DbxWebAuthNoRedirect auth = new DbxWebAuthNoRedirect(config, info);
        String authUrl = auth.start();
        
        // Open the first-time auth dialog (either web or desktop).
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(authUrl));
            } catch (URISyntaxException | IOException e){
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(null, "1. Go to: " + authUrl +
                "\n2. Click \"Allow\" (you might have to log in first)\n3. Copy the authorization code.");
        }
        
        String code = JOptionPane.showInputDialog("Enter the Authorization Code");
        
        // Retrieve the access token from the authorization dialog.
        try {
            DbxAuthFinish finish = auth.finish(code);
            authToken = finish.accessToken;
        } catch (DbxException e) {
            JOptionPane.showMessageDialog(null,"Invalid Token!");
            e.printStackTrace();
            System.exit(1);
        }
        
        // Write the access token to our file for future use.
        try (FileWriter fw = new FileWriter(authFile)) {
            fw.write(authToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void readTagFile() {
        // Just in case if we are reloading.
        tags.clear();
        for (WatchKey key : watchedFiles) {
            key.cancel();
        }
        watchedFiles.clear();
                
        // Parse the tag file and watch our local data.
        try (BufferedReader br = new BufferedReader(new FileReader(new File(tagFilePath)))){
            String line;
            while ((line = br.readLine()) != null) {
                // Load tags.
                String cloudPath = line.split(";")[0];
                String localPath = line.split(";")[1];
                tags.add(cloudPath, localPath);
                
                // Start watching local files.
                WatchKey key;
                File target = new File(localPath);
                if (target.isDirectory()) {
                    key = target.toPath().register(watcher, ENTRY_CREATE,
                                                   ENTRY_DELETE, ENTRY_MODIFY);
                    
                    recursiveWatch(localPath);
                } else {
                    key = target.toPath().getParent().register(watcher,
                                                               ENTRY_CREATE,
                                                               ENTRY_DELETE,
                                                               ENTRY_MODIFY);
                }
                
                watchedFiles.add(key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        tags.printContents();
    }
    
    private void recursiveWatch(String rootPath) {
        File root = new File(rootPath);

        for (File sub : root.listFiles()) {
            try {
                if (sub.isDirectory()) {
                    WatchKey key = sub.toPath().register(watcher, ENTRY_CREATE,
                                       ENTRY_DELETE, ENTRY_MODIFY);
                    watchedFiles.add(key);
                    recursiveWatch(sub.getAbsolutePath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void writeTagFile() {
        
    }
    /**
     * Uploads a file
     * @param file the file to upload
     * @return true if successful
     */
    public void upload(File file) throws DbxException, IOException{
        String cloudPath = tags.getCloudPath(file.toString());
        System.err.println("Local file modified: " + cloudPath);
        FileInputStream fis = new FileInputStream(file);
        DbxEntry.File uploaded = client.uploadFile(cloudPath, DbxWriteMode.update(""), file.length(), fis);   
    }
    @Override
    /**
     * If there are changes on local side, upload to dropbox
     */
    public void syncLocalChanges() {
        for (WatchKey key : watchedFiles) {
            System.err.println("Key: " + (Path)(key.watchable()));
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
                WatchEvent.Kind<?> kind = pathEvent.kind();
                
                String dpath = ((Path)key.watchable()).toString();
                String fpath = dpath + "\\" + pathEvent.context();
                
                if (kind == ENTRY_DELETE) {
                    // If the gui option is selected, delete the local file or do nothing
                } else if (kind == ENTRY_MODIFY) {
                    // If this file is tagged, then upload it to the cloud.
                    String cloudPath = tags.getCloudPath(fpath);
                    System.err.println("Local file modified: " + cloudPath);
                    
                    if (cloudPath != null) {
                        File file = new File(fpath);
                        try{
                            upload(file);
                        }catch(IOException | DbxException e){
                            e.printStackTrace();
                            uploadList.add(file);
                        }
                        /*
                        try (FileInputStream fis = new FileInputStream(file)) {
                            DbxEntry.File uploaded = client.uploadFile(cloudPath, DbxWriteMode.update(""), file.length(), fis);
                        } catch (DbxException | IOException e) {
                            e.printStackTrace();
                            uploadList.add(file);
                        }*/
                    }
                }
            }
            
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }
    
    @Override
    /**
     * If there are changes on dropbox side, then make changes on the local side
     */
    public void syncRemoteChanges() {
        for (DbxDelta.Entry delta : changes.entries) {
            System.err.println("Downloading Files");
            if (delta.metadata == null) {
                // Entry was deleted on dropbox
                System.out.println("File Not Found on Dropbox! - Uploading");
            } else {
                // Entry is there
                try {
                    DbxEntry ent = client.getMetadata(delta.lcPath);
                    if (ent.isFile()) {
                        try (FileOutputStream fos = new FileOutputStream(tags.getLocalPath(ent.path))) {
                            client.getFile(ent.path, null, fos); //Writes file from cloud to ground
                        } catch (DbxException | IOException e) {
                            throw e;
                        }
                    }
                } catch (DbxException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    @Override
    public void waitForChanges() {
        boolean fileChanged = false;
        while (!fileChanged) {
            try {
                //System.out.println("Waiting for changes");
                Thread.sleep(1000);
                pushInterruptedFiles();
                //Remote Changes
                try {
                    changes = client.getDelta(returnedFiles);
                    returnedFiles = changes.cursor;
                    if (!changes.entries.isEmpty()) {
                        fileChanged = true;
                    }
                } catch (DbxException e) {
                    e.printStackTrace();
                    System.err.println("[REMOTE CHANGE ERROR] Check internet connection");
                }
                
                //Local Changes
                WatchKey w = watcher.poll();
                if (w != null) {
                    System.err.println("Key: " + (Path)(w.watchable()));
                    fileChanged = true;
                }
            } catch(InterruptedException e){
                e.printStackTrace();
                System.err.println("[LOCAL CHANGE ERROR]");
            }
        }
    }
    @Override
    public void pushInterruptedFiles() {
        LinkedList<Integer> indiciesToRemove = new LinkedList<>();
        int i = 0;
        for(File file: uploadList){
            try{
                upload(file);
                indiciesToRemove.add(i);
                //uploadList.remove(file);//Concurrent modification error
            }catch(DbxException | IOException e){
                System.out.println("Could not upload file "+file.toString()+" retrying...");
            }
            i++;
        }
        for(Integer num: indiciesToRemove){
            uploadList.remove(num.intValue());
        }
    }
}
