/*
 * Copyright (C) 2016 PushBuddy Development Team
 */
package pushbuddy;

import javax.swing.UIManager;
import java.io.File;
import java.io.FileWriter;

/**
 * The PushBuddy desktop client.
 * 
 * @author Brandon Wong
 * @author Eyal Kalderon
 */
public class PushBuddy {
    public static final String name = "PushBuddy";
    public static Dropbox db;
    public static void startGUI() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                /*if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }*/
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PushBuddyForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PushBuddyForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PushBuddyForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PushBuddyForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
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
        createStartUp();
        db = new Dropbox("DropboxTags.txt", "dropboxAccess.txt");
        CloudThread svc1 = new CloudThread(db);
        
        svc1.start();
        startGUI();
        svc1.join();
    }
    public static void createStartUp(){
        if(System.getProperty("os.name").contains("Windows")){
            String filepath = (System.getProperty("user.home")+"\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\PushBuddyStart.bat");
            File startup = new File(filepath);
            if(!startup.exists()){
                try(FileWriter fw = new FileWriter(startup)){
                    fw.write("cd C:\\Program Files (x86)\\PushBuddy\\PushBuddy\n");
                    fw.write("java -jar PushBuddy.jar");
                }catch(java.io.IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
