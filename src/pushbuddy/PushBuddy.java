/*
 * Copyright (C) 2016 PushBuddy Development Team
 */
package pushbuddy;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * The PushBuddy desktop client.
 * 
 * @author Brandon Wong
 * @author Eyal Kalderon
 */
public class PushBuddy {
    public static final String name = "PushBuddy2";
    public static ArrayList<CloudThread> services = new ArrayList<>();
    public static String os;
    
    /**
     * The main thread of the program.
     */
    public static void main(String[] args) throws Exception {
		whatOS();
        //createStartupScript();
        //Temporary
		Dropbox dbx = new Dropbox("Dropbox", "www.dropbox.com");
		//dbx.authenticate();
		
        services.add(new CloudThread(dbx));

		
        services.stream().forEach(srv -> srv.start());
        
		
		Thread t = new Thread(){
			public void run(){
				Gui.startGUI(args);
			}
		};
        t.start();
		
		
    }
    
	private static void whatOS(){//TODO make better
		if (System.getProperty("os.name").contains("Windows"))
            os = "Windows";
		else
			os = "unix";
	}
	
	/*
    public static void createStartupScript(){
        if (System.getProperty("os.name").contains("Windows")) {
            os = "Windows";
            String roamingFolder = System.getenv("APPDATA");
            String scriptPath = "\\Microsoft\\Windows\\Start Menu\\Programs"
                              + "\\Startup\\PushBuddyStart.bat";
            
            File script = Paths.get(roamingFolder + scriptPath).toFile();
            if (!script.exists()) {
                try (FileWriter fw = new FileWriter(script)) {
                    fw.write("cd C:\\Program Files (x86)\\PushBuddy Dev\\PushBuddy0.3\n");
                    fw.write("java -jar PushBuddy.jar");
                } catch(java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/
    
    
}
