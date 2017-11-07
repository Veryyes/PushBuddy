/*
 * Copyright (C) 2017 PushBuddy Development Team
 */
 
package pushbuddy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import java.util.List;
import java.util.LinkedList;

import javax.swing.JFileChooser;

/**
 * The PushBuddy GUI
 *
 * @author Brandon Wong
 */
public class Gui extends Application {
    
	
    @Override
    public void start(Stage stage) throws IOException{
		/*
			javafx has FileChooser and DirectoryChooser, nothing
			that supports both at the same time, so we are stuck with the
			javax.swing support for now
			
			TODO write out own. :)
		*/
		//FileChooser fileChooser = new FileChooser();
		//fileChooser.setTitle("Pick a file or directory to sync!");
		JFileChooser chooser = new JFileChooser(".");
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		
		Parent root = FXMLLoader.load(getClass().getResource("pushbuddy2.fxml"));
		Scene scene = new Scene(root, 300, 485);
		stage.setScene(scene);
		stage.getIcons().add(new Image("file:../../res/pushbuddyicon.png"));
		stage.setTitle("PushBuddy 2.0");
		stage.setResizable(false);
		stage.show();
		
		Button tagBtnDbx = (Button) scene.lookup("#tagBtnDbx");
		tagBtnDbx.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				//List<File> 
				File[] selectedFiles;
				chooser.showOpenDialog(null);
				selectedFiles = chooser.getSelectedFiles();
				//fileChooser.showOpenMultipleDialog(stage);
				for(File f: selectedFiles){
					System.out.println("Syncing: "+ f);
					Cloud dbx = PushBuddy.services.get(0).getCloud();
					File tagFile = dbx.getTags().getTagFile();
					try(FileWriter fw = new FileWriter(tagFile, true)){
						fw.write("/"+f.getName()+";"+f.getAbsolutePath()+"\n");
						System.out.println(f.getName());
					}catch (IOException e1){
						System.out.println("Failed to tag file: "+ f.getName());
						e1.printStackTrace();
					}
				}
				System.out.println(e);
			}
		});
		
		Button viewBtnDbx = (Button) scene.lookup("#viewBtnDbx");
		viewBtnDbx.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				System.out.println(e);
			}
		});
    }
	
	public static void startGUI(String[] args) {
        launch(args);
    }
	
}