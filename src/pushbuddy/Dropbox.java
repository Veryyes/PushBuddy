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
    
    
    public Dropbox(String tagFile, String authFilePath) {
        super(tagFile, authFilePath); 
    }
    
    @Override
    public void authenticate() {
        DbxAppInfo info = new DbxAppInfo(APP_KEY, APP_SECRET);
        DbxRequestConfig config = new DbxRequestConfig("PushBuddy/1.0",
            Locale.getDefault().toString());
        
        if (authFile.exists()) {
            useExistingAuth();
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
    
    /**
     * Uploads a file
     * @param file the file to upload
     * @return true if successful
     */
    public void upload(File file) throws DbxException, IOException{
        if(file.isFile()){
            System.out.println(file);
            String cloudPath = tags.getRemotePath(file.toPath());
            System.err.println("Local file modified: " + cloudPath);
            FileInputStream fis = new FileInputStream(file);
            DbxEntry.File uploaded = client.uploadFile(cloudPath, DbxWriteMode.update(""), file.length(), fis);
        }
    }
    @Override
    /**
     * If there are changes on local side, upload to dropbox
     */
    public void syncLocalChanges() {
        for (Path p: tags.getLocalFiles()) {
            if (!existsOnCloud(p)) {
                
                File file = new File(p.toUri());
                try {
                    if (!file.isDirectory()) {
                        upload(file);
                    }
                } catch (IOException | DbxException e) {
                    e.printStackTrace();
                }
            }
        }
        for (WatchKey key : tags.getWatchedDirs()) {
            //System.err.println("Key: " + (Path)(key.watchable()));
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
                WatchEvent.Kind<?> kind = pathEvent.kind();
                
                String dpath = ((Path)key.watchable()).toString();
                String fpath = dpath + File.separator + pathEvent.context();
                
                if (kind == ENTRY_DELETE) {
                    // If the gui option is selected, delete the local file or do nothing
                } else if (kind == ENTRY_MODIFY) {
                    // If this file is tagged, then upload it to the cloud.
                    if (existsOnCloud(Paths.get(fpath))) {
                        System.out.println("Uploading Local Changes");
                        File file = new File(fpath);
                        try {
                            if (!file.isDirectory()) {
                                upload(file);
                            }
                        } catch (IOException | DbxException e) {
                            e.printStackTrace();
                        }
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
            //System.err.println("Downloading Files");
            if (delta.metadata == null) {
                // Entry was deleted on dropbox
                System.out.println("File Not Found on Dropbox! - Uploading");
            } else {
                // Entry is there
                try {
                    DbxEntry ent = client.getMetadata(delta.lcPath);
                    if (ent.isFile()) {
                        Path remote = Paths.get(ent.path);
                        String local = tags.getLocalPath(remote).toString();
                        
                        try (FileOutputStream fos = new FileOutputStream(local)) {
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
                if(tags.localFilesChanged()){
                    fileChanged = true;
                }else if(tags.tagFileChanged()){
                    tags.clear();
                    tags.rebuildData();
                    fileChanged = true;
                }
            } catch(InterruptedException e){
                e.printStackTrace();
                System.err.println("[LOCAL CHANGE ERROR]");
            }
        }
    }
    
    @Override
    public void pingService(){
        boolean canReach = false;
        while(!canReach){
            try(Socket socket = new Socket()){
                socket.connect(new InetSocketAddress("dropbox.com",80),2000);
                canReach = true;
            }catch(IOException e){
                canReach = false;
            }
            System.out.println(canReach);
        }
    }
    public boolean existsOnCloud(Path path){
        try{
            //System.out.println(client.getMetadata(tags.getRemotePath(path).toString()));
            client.getMetadata(tags.getRemotePath(path));
            return true;
        }catch(DbxException | IllegalArgumentException e){
            return false;
        }
    }
}
