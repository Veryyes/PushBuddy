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
import java.io.IOException;
import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;

/**
 * The PushBuddy GUI
 *
 * @author Brandon Wong
 */
public class Gui extends Application {
    
    @Override
    public void start(Stage stage) throws IOException{
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