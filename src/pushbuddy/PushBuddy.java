/*
 * Copyright (C) 2016 PushBuddy Development Team
 */
package pushbuddy;

import javax.swing.UIManager;

/**
 * The PushBuddy desktop client.
 * 
 * @author Brandon Wong
 * @author Eyal Kalderon
 */
public class PushBuddy {
    public static final String name = "PushBuddy";
    
    public static void startGUI() {        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PushBuddyForm().setVisible(true);
            }
        });
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        Dropbox db = new Dropbox("DropboxTags.txt", "dropboxAccess.txt");
        CloudThread svc1 = new CloudThread(db);
        
        svc1.start();
        startGUI();
        svc1.join();
    }
}
