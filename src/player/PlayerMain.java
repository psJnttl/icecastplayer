package player;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import player.model.RadioStation;
import player.view.PlayerMainController;
import player.view.RootLayoutController;

public class PlayerMain extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    private ObservableList<RadioStation> stationList = FXCollections.observableArrayList();
    private String streamToFileName;
    private RandomAccessFile streamToRaf = null;
    private PlayerMainController playerMainController;

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

            RootLayoutController controller = loader.getController();
            controller.setPlayerMain(this);

            primaryStage.show();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPlayerMain() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(PlayerMain.class.getResource("view/PlayerMainLayout.fxml"));
            AnchorPane playerMainView = (AnchorPane) loader.load();
            rootLayout.setCenter(playerMainView);

            playerMainController = loader.getController();
            playerMainController.setPlayerMain(this);
            playerMainController.setStationSelectList(stationList);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStreamToFile(File file) {
        this.streamToFileName = file.getAbsolutePath();
    }

    public void setStreamToFile(String filename) {
        this.streamToFileName = filename;
    }

    public void playStream(RadioStation station) {
        if (null != station && !station.getStationUrl().isEmpty()) {
            playerMainController.setDisableFileSelection(true);
            System.out.println("now Playing " + station.getName());
            try {
                if (null != streamToRaf) {
                    streamToRaf.close();
                }
                if (null != streamToFileName && !streamToFileName.isEmpty()) {
                    streamToRaf = new RandomAccessFile(this.streamToFileName, "rw");
                    streamToRaf.writeBytes("mp3 or ogg");
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopStream() {
        try {
            if (null != streamToRaf) {
                streamToRaf.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        playerMainController.setDisableFileSelection(false);
    }

    public void shutDown() {
        System.out.println("shutDown()");
        if (null != streamToRaf) {
            try {
                streamToRaf.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() { // in case app closed by closing window
        shutDown();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
