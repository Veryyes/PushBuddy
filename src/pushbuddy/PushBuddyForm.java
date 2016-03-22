/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pushbuddy;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFrame;

/**
 *
 * @author Rocky
 */
public class PushBuddyForm extends javax.swing.JFrame {

    /**
     * Creates new form PushBuddyForm
     */
    public PushBuddyForm() {
        initComponents();
        //setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(PushBuddyForm.class.getResource("Final_pushbuddy_finalish_32px.png")));
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        fileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setControlButtonsAreShown(false);
        serviceBox = new javax.swing.JComboBox<>();
        tagBtn = new javax.swing.JButton();
        doneBtn = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();

        setTitle("PushBuddy");
        setIconImage(Toolkit.getDefaultToolkit().getImage(PushBuddyForm.class.getResource("Final_pushbuddy_32px.png")));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileChooserActionPerformed(evt);
            }
        });

        serviceBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Please Select a Cloud Service", "Dropbox" }));

        tagBtn.setText("Tag");
        tagBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagBtnActionPerformed(evt);
            }
        });

        doneBtn.setText("Done");
        doneBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneBtnActionPerformed(evt);
            }
        });

        jLabel2.setText("Service:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(42, 42, 42)
                        .addComponent(serviceBox, 0, 391, Short.MAX_VALUE)
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(doneBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                            .addComponent(tagBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(21, 21, 21))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fileChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileChooser, javax.swing.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serviceBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(tagBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(doneBtn)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void doneBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doneBtnActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_doneBtnActionPerformed

    private void tagBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagBtnActionPerformed
        String service = (String)serviceBox.getSelectedItem();
        java.io.File[] selectedFiles = fileChooser.getSelectedFiles();
        java.io.File startPath = fileChooser.getCurrentDirectory();
        switch(service){
            case "Dropbox":
                try(FileWriter fw = new FileWriter("DropboxTags.txt",true)){
                    Cloud cloud = PushBuddy.services.get(0).getCloud();//TODO want a not hardcoded way to get Dropbox Obj
                    for(java.io.File f:selectedFiles){
                        String remotePath = "/"+f.getName();

                        if(cloud.isDuplRemote(remotePath)){//Check for the first duplicate copy
                            int extensionIndex = remotePath.indexOf('.');//TODO this doesnt work for things named like ".gitignore"
                            if(extensionIndex>=0){
                                String newRemotePath = remotePath.substring(0,extensionIndex)+"(1)";
                                String extension = remotePath.substring(extensionIndex);
                                remotePath = newRemotePath + extension;
                            }else{
                                remotePath+="(1)";
                            }
                        }
                        
                        
                        for(int i=2;cloud.isDuplRemote(remotePath);i++){ //Keep checking if there is more than 2 duplicates 
                            
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
                        fw.write(remotePath+";"+f.getAbsolutePath()+"\n");
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
                System.out.println("Finished Writing to file");
                break;
            case "Google Drive":
                try(FileWriter fw = new FileWriter("GoogleDriveTags.txt",true)){
                    for(java.io.File f:selectedFiles)
                        fw.write("/"+f.getName()+";"+f.getAbsolutePath()+"\n");
                }catch(IOException e){
                    e.printStackTrace();
                }
                System.out.println("Finished Writing to file");
                break;
            default:
                javax.swing.JOptionPane.showMessageDialog(this, "Please select a cloud service to upload to");
        }
        fileChooser.setCurrentDirectory(startPath);
        
    }//GEN-LAST:event_tagBtnActionPerformed

    private void fileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileChooserActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fileChooserActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        CloudThread.running = false;
    }//GEN-LAST:event_formWindowClosed

    private void formWindowDeactivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowDeactivated
        //CloudThread.running = false;
        //System.out.println("Deactivate");
    }//GEN-LAST:event_formWindowDeactivated

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        //TrayIcon
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        Image ico = Toolkit.getDefaultToolkit().getImage(PushBuddyForm.class.getResource("pushbuddyTaskBar.png"));
        ico = ico.getScaledInstance(16, 16, Image.SCALE_DEFAULT);
        final TrayIcon trayIcon =
                new TrayIcon(ico, "PushBuddy");
        final SystemTray tray = SystemTray.getSystemTray();
        
        MenuItem openItem = new MenuItem("Open");
        openItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                setVisible(true);
            }
        });
        JFrame ref = this;
        MenuItem closeItem = new MenuItem("Close");
        closeItem.addActionListener(new ActionListener(){
           public void actionPerformed(ActionEvent evt) {
               //dispatchEvent(new WindowEvent(ref, WindowEvent.WINDOW_CLOSING));
               //ref.dispose();
               CloudThread.running = false;
               System.exit(0);
           }
        });
        
        popup.add(openItem);
        popup.addSeparator();
        popup.add(closeItem);
        
        trayIcon.setPopupMenu(popup);
        try{
            tray.add(trayIcon);
        }catch(AWTException e){
            e.printStackTrace();
        }
    }//GEN-LAST:event_formWindowOpened
    
    
    private java.io.File[] selectedFiles;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton doneBtn;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JComboBox<String> serviceBox;
    private javax.swing.JButton tagBtn;
    // End of variables declaration//GEN-END:variables
}
