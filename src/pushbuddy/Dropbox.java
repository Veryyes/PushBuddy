package pushbuddy;

import com.dropbox.core.*;
import com.dropbox.core.v2.*;
import com.dropbox.core.v2.users.*;
import com.dropbox.core.v2.files.*;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Locale;
import javax.swing.JOptionPane;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Provides Dropbox cloud API support.
 * 
 * @author Eyal Kalderon
 * @author Brandon Wong
 */
public class Dropbox extends Cloud {
    private final static String APP_KEY =    "zy2t9yo9ej2tmcu";
    private final static String APP_SECRET = "bkrwaat5xo16bek"; 
    private String accessToken;
    private DbxClientV2 client;
	private DbxRequestConfig config;
	private DbxAppInfo info;
	private DbxUserFilesRequests fileRequests;
 //   private DbxDelta<DbxEntry> changes = null;
    private String returnedFiles = null;
	
    public Dropbox(String tagFile, String authFilePath) {
        super(tagFile, authFilePath);
		//fileRequests = new DbxUserFilesRequests(client);
    }
    
	public void test(){
		try{
			File f = new File("C:/Users/Brandon/Desktop/46039.png");
			tags.add(f.toPath());
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
    @Override
    public void authenticate() {
		config = new DbxRequestConfig("PushBuddy2");
        info = new DbxAppInfo(APP_KEY, APP_SECRET);

        
        if (authFile.exists()) {
            useExistingAuth();
        } else {
            genNewAuth(config, info);
        }
		
        client = new DbxClientV2(config, accessToken);
		try{
			FullAccount account = client.users().getCurrentAccount();
			System.out.println("Logged in as: " + account.getName().getDisplayName());
			JOptionPane.showMessageDialog(null, "Successfully logged into PushBuddy as: " + account.getName().getDisplayName());
		}catch (DbxException e){
			System.out.println("Failed to login");
			e.printStackTrace();
			enabled = false;
		}
        
		// Check for any changes right off the bat
        /*try {
            changes = client.getDelta(returnedFiles);
        } catch (DbxException e) {
            e.printStackTrace();
        }*/

    }
    
    private void useExistingAuth() {
        try (BufferedReader br = new BufferedReader(new FileReader(authFile))) {
            accessToken = br.readLine();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void genNewAuth(DbxRequestConfig config, DbxAppInfo info) {
		DbxWebAuth auth = new DbxWebAuth(config, info);
		DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder().withNoRedirect().build();
		
		String authURL = auth.authorize(authRequest);
		
		boolean haveRightCode = false;
		while(!haveRightCode){
			// Open the first-time auth dialog (either web or desktop).
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(authURL));
				} catch (URISyntaxException | IOException e){
					e.printStackTrace();
				}
			} else {
				JOptionPane.showMessageDialog(null, "1. Go to: " + authURL +
					"\n2. Click \"Allow\" (you may have to log in first)\n3." +
					"Copy the authorization code.");
			}
			
			// Enter in Authorization Code
			String code = JOptionPane.showInputDialog("Enter the Authorization Code");
		
		// Parse & Prepare Authorization Code
			if(code != null && code.length() > 0){
				try{
					code = code.trim();
					DbxAuthFinish authFinish = auth.finishFromCode(code);
					accessToken = authFinish.getAccessToken();
					haveRightCode = true;
				}catch(DbxException e){
					System.out.println("Unable to communicate with Dropbox or bad Code");
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Bad Auth Token. Please Try again.");
				}
			}else{
				JOptionPane.showMessageDialog(null,"Invalid Token!");
				System.exit(1);
			}
		}
        
        // Write the access token to our file for future use.
        try (FileWriter fw = new FileWriter(authFile)) {
            fw.write(accessToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Uploads a file to the cloud.
     * @param file the local target file
     * @throws DbxException, IOException
     */
    private void upload(File file) throws DbxException, IOException {	//TODO consider creating abstract version on Cloud.java
        if (file.isFile()) {
            String cloudPath = tags.getRemotePath(file.toPath());
            System.err.println("Uploading " + file+" to CloudPath: "+cloudPath);
			InputStream in = new FileInputStream(file);
			FileMetadata metadata = client.files().uploadBuilder(cloudPath).uploadAndFinish(in);
			in.close();
        }
    }
    
    @Override
    public void syncLocalChanges() { //TODO consider migration to Cloud.java
        for (Path p : tags.getLocalFiles()) {
            if (!existsOnCloud(p)) {
                File file = p.toFile();
                
                try {
                    upload(file);
                } catch (IOException | DbxException e) {
                    e.printStackTrace();
                }
            }
        }
        
        for (WatchKey key : tags.getWatchedDirs()) {
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
                WatchEvent.Kind<?> kind = pathEvent.kind();
                
                String dpath = ((Path)key.watchable()).toString();
                String fpath = dpath + File.separator + pathEvent.context();
                
                if (kind == ENTRY_DELETE) {
                    // If the gui option is selected, either  delete the local
                    // file or do nothing.
                } else {
                    // If this is a file:
                    //    If this file is tagged, then upload it to the cloud.
                    // If this is a directory:
                    //    If it contains any tagged files, upload them to the cloud.
                    File file = new File(fpath);
                    if (file.isFile() && existsOnCloud(file.toPath())) {
                        try {
                            upload(file);
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
    public void syncRemoteChanges() {/*
        for (DbxDelta.Entry delta : changes.entries) {
            // Entry was deleted.
            if (delta.metadata == null) {
                System.out.println("File was deleted! What do you want to do?");
                continue;
            }
            
            // Entry still exists on cloud.
            try {
                DbxEntry ent = client.getMetadata(delta.lcPath);
                if (ent.isFile()) {
                    Path local = tags.getLocalPath(ent.path);
                    
                    if (local == null) {
                        System.err.println(ent.path + " doesn't exist locally! Ignoring.");
                        continue;
                    }

                    try (FileOutputStream fos = new FileOutputStream(local.toString())) {
                        client.getFile(ent.path, null, fos);
                    } catch (DbxException | IOException e) {
                        throw e;
                    }
                }
            } catch (DbxException | IOException e) {
                e.printStackTrace();
            }
        }*/
    }
    
    @Override
    public boolean remoteFilesChanged() {
		/*
        try {
            changes = client.getDelta(returnedFiles);
            returnedFiles = changes.cursor;
            if (!changes.entries.isEmpty()) {
                return true;
            }
        } catch (DbxException e) {
            e.printStackTrace();
            System.err.println("[REMOTE CHANGE ERROR] Check internet connection");
        }
        */
        return false;
    }
    
    public boolean existsOnCloud(Path path){//TODO consider creating abstract version on Cloud.java
        /*try {
			return client.files().getMetaData(tags.getRemotePath(path)) != null;
        } catch (DbxException | IllegalArgumentException | GetMetadataErrorException e) {
			e.printStackTrace();
            return false;
        }*/
		return false;
    }
}
