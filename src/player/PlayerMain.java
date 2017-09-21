package player;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import player.model.RadioStation;
import player.model.StationListWrapper;
import player.view.PlayerMainController;
import player.view.RootLayoutController;


import jouvieje.bass.BassInit;
import jouvieje.bass.exceptions.BassException;
import static jouvieje.bass.Bass.*;

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
        initBassNative();
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

    public void playStream(String stationUrl) {
        if (null != stationUrl && !stationUrl.isEmpty()) {
            playerMainController.setDisableFileSelection(true);
            System.out.println("now Playing " + stationUrl);
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
        closeBassNative();
    }

    public void stop() { // in case app closed by closing window
        shutDown();
    }

    public void loadStationList(File file) {
        System.out.println("station list file: " + file.getAbsolutePath());
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(StationListWrapper.class);
            Unmarshaller unmarshaler = jaxbContext.createUnmarshaller();
            StationListWrapper stationListWrapper = (StationListWrapper) unmarshaler.unmarshal(file);
            stationList.clear();
            stationList.addAll(stationListWrapper.getStationList());
            playerMainController.setStationSelectList(stationList);
        }
        catch (JAXBException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setHeaderText("Couldn't load stations from XML file.");
            alert.setContentText(e.getCause().toString() + "\n" + e.getMessage());
        }

    }

    public void saveStationList(File file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(StationListWrapper.class);
            Marshaller marshaler = jaxbContext.createMarshaller();
            marshaler.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StationListWrapper stationListWrapper = new StationListWrapper();
            stationListWrapper.setStationList(stationList);
            marshaler.marshal(stationListWrapper, file);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    void initBassNative() {
        try {
            BassInit.loadLibraries();
        }
        catch (BassException e) {
            System.out.println("Failed to load Bass library, " + e.getMessage());
            return;
        }
        if (BassInit.NATIVEBASS_LIBRARY_VERSION() != BassInit.NATIVEBASS_JAR_VERSION()) {
            System.out.println("Library version does not match."); 
            System.out.println("lib version: " + BassInit.NATIVEBASS_LIBRARY_VERSION());
            System.out.println("JAR version: " + BassInit.NATIVEBASS_JAR_VERSION());
            return;
        }
    }
    void closeBassNative() {
        BASS_Free();
    }
}
