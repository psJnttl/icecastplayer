package player;

import java.io.IOException;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import player.model.RadioStation;

public class PlayerMain extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    private ObservableList<RadioStation> stationList = FXCollections.observableArrayList();

    public PlayerMain() {
        stationList.add(new RadioStation("80splanet.com", "http://23.92.61.227:9020"));
        stationList.add(new RadioStation("ROCK ANTENNE Heavy Metal", "http://mp3channels.webradio.antenne.de:80/heavy-metal"));
        stationList.add(new RadioStation("RADIO ENERGY (NRJ), 90s", "http://stream.radioreklama.bg:80/nrj128"));
        stationList.add(new RadioStation("Breakz.FM - DJ Radio", "http://radio2.breakz.fm:8000/stream128.mp3"));
    }

    public ObservableList<RadioStation> getStationList() {
        return stationList;
    }

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
