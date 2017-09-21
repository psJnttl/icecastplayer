package player;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class PlayerMain extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;

    public Stage getPrimaryStage() {
        return this.primaryStage;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("NetRadioPlayer");
        itializeRootLayout();
        showPlayerMain();
    }

    private void itializeRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(PlayerMain.class.getResource("view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void showPlayerMain() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(PlayerMain.class.getResource("view/PlayerMain.fxml"));
            AnchorPane playerMainView = (AnchorPane) loader.load();
            rootLayout.setCenter(playerMainView);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
