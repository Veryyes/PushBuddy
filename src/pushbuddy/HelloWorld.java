import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import java.io.IOException;


public class HelloWorld extends Application {
    
    @Override
    public void start(Stage stage) throws IOException{
		Parent root = FXMLLoader.load(getClass().getResource("pushbuddy2.fxml"));
		Scene scene = new Scene(root, 300, 485);
		stage.setScene(scene);
		stage.setTitle("PushBuddy 2.0");
		stage.show();
		
		ImageView im = (ImageView) scene.lookup("#upImg_dbx");
    }
	
	public static void main(String[] args) {
        launch(args);
    }
}