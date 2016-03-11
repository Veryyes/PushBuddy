/*
 * Copyright (C) 2016 PushBuddy Development Team
 */
package pushbuddy;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.*;
import java.util.ArrayList;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * The PushBuddy desktop client.
 * 
 * @author Brandon Wong
 * @author Eyal Kalderon
 */
public class PushBuddy {
    public static final String name = "PushBuddy";
    public static ArrayList<CloudThread> services = new ArrayList();
    
    /**
     * The main thread of the program.
     */
    public static void main(String[] args) throws InterruptedException {
        createStartupScript();
        
        services.add(new CloudThread(new Dropbox("DropboxTags.txt", "dropboxAccess.txt")));
        
        for (CloudThread srv : services) {
            srv.start();
        }
        
        startGui();
        
        for (CloudThread srv : services) {
            srv.join();
        }
    }
    
    public static void createStartupScript(){
        if (System.getProperty("os.name").contains("Windows")) {
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
    }
    
    public static void startGui() {
        try {
            String theme = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(theme);
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Could not find native theme! Using default...");
        } catch (Exception e) {
            e.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PushBuddyForm().setVisible(true);
            }
        });
    }
}
