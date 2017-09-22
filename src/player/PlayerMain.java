package player;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import player.model.RadioStation;
import player.model.StationListWrapper;
import player.view.PlayerMainController;
import player.view.RootLayoutController;

import static jouvieje.bass.Bass.*;
import jouvieje.bass.BassInit;
import jouvieje.bass.exceptions.BassException;
import jouvieje.bass.structures.HSTREAM;
import jouvieje.bass.structures.HSYNC;
import jouvieje.bass.utils.Pointer;
import player.util.Device;

import static jouvieje.bass.defines.BASS_STREAM.BASS_STREAM_BLOCK;
import static jouvieje.bass.defines.BASS_STREAM.BASS_STREAM_STATUS;
import static jouvieje.bass.defines.BASS_STREAM.BASS_STREAM_AUTOFREE;
import jouvieje.bass.callbacks.DOWNLOADPROC;
import jouvieje.bass.callbacks.SYNCPROC;
import static jouvieje.bass.defines.BASS_FILEPOS.BASS_FILEPOS_BUFFER;
import static jouvieje.bass.defines.BASS_FILEPOS.BASS_FILEPOS_END;
import static jouvieje.bass.defines.BASS_FILEPOS.BASS_FILEPOS_CONNECTED;
import static jouvieje.bass.defines.BASS_TAG.BASS_TAG_HTTP;
import static jouvieje.bass.defines.BASS_TAG.BASS_TAG_ICY;
import static jouvieje.bass.defines.BASS_TAG.BASS_TAG_META;
import static jouvieje.bass.defines.BASS_TAG.BASS_TAG_OGG;
import static jouvieje.bass.defines.BASS_SYNC.BASS_SYNC_META;
import static jouvieje.bass.defines.BASS_SYNC.BASS_SYNC_OGG_CHANGE;
import static jouvieje.bass.defines.BASS_SYNC.BASS_SYNC_END;
import static jouvieje.bass.defines.BASS_CONFIG.BASS_CONFIG_NET_PLAYLIST;
import static jouvieje.bass.defines.BASS_CONFIG.BASS_CONFIG_NET_PREBUF;
import static jouvieje.bass.defines.BASS_CONFIG_NET.BASS_CONFIG_NET_PROXY;
import static jouvieje.bass.defines.BASS_ATTRIB.BASS_ATTRIB_VOL;

public class PlayerMain extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    private ObservableList<RadioStation> stationList = FXCollections.observableArrayList();
    private String streamToFileName;
    private RandomAccessFile streamToRaf = null;
    private PlayerMainController playerMainController;
    private Thread streamingThread = null;
    private HSTREAM chan = null;
    private boolean muteState = false;

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
                openStreamUrl(stationUrl);
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
        BASS_ChannelStop(chan.asInt());
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

    private void initBassNative() {
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
        if (((BASS_GetVersion() & 0xFFFF0000) >> 16) != BassInit.BASSVERSION()) {
            System.out.println("An incorrect version of BASS.DLL was loaded");
            return;
        }
        if (!BASS_Init(Device.forceNoSoundDevice(-1), Device.forceFrequency(44100), 0, null, null)) {
            System.out.println("Can't initialize device");
            shutDown();
        }
        BASS_SetConfig(BASS_CONFIG_NET_PLAYLIST, 0); // Disable PLS, M3U
                                                     // playlist. Future option
                                                     // bookmark.
        BASS_SetConfig(BASS_CONFIG_NET_PREBUF, 0); // enable buffering status
                                                   // display by setting
                                                   // automatic buffering to
                                                   // minimum
        BASS_SetConfigPtr(BASS_CONFIG_NET_PROXY, null);
    }

    private void closeBassNative() {
        BASS_Free();
    }

    private void openStreamUrl(String stationUrl) {
        if (null != streamingThread) {
            System.out.println("Thread already running");
            return;
        }
        streamingThread = new Thread() {
            public synchronized void start() {
                System.out.println("Starting streaming thread");
                streamBufferTimer.stop();
                BASS_StreamFree(chan);
                chan = BASS_StreamCreateURL(stationUrl, 0,
                        BASS_STREAM_BLOCK | BASS_STREAM_STATUS | BASS_STREAM_AUTOFREE, statusProc, null);
                if (null == chan) {
                    System.out.println("Failed to play stream: " + stationUrl);
                }
                else {
                    streamBufferTimer.play();
                }
                streamingThread = null;
            }
        };
        streamingThread.start();
    }

    private Timeline streamBufferTimer = new Timeline(
            new KeyFrame(Duration.millis(50), new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    int progress = (int) (BASS_StreamGetFilePosition(chan, BASS_FILEPOS_BUFFER) * 100
                            / BASS_StreamGetFilePosition(chan, BASS_FILEPOS_END));
                    if (progress > 75 || BASS_StreamGetFilePosition(chan, BASS_FILEPOS_CONNECTED) != 0) {
                        streamBufferTimer.stop();
                        Pointer tags = BASS_ChannelGetTags(chan.asInt(), BASS_TAG_ICY);
                        if (null == tags) {
                            tags = BASS_ChannelGetTags(chan.asInt(), BASS_TAG_HTTP);
                        }
                        if (null != tags) {
                            int length;
                            while ((length = tags.asString().length()) > 0) {
                                final String ICY_NAME = "icy-name:";
                                final String ICY_URL = "icy-br:";

                                String tag = tags.asString();
                                if (tag.toLowerCase().startsWith(ICY_NAME)) {
                                    // set message from station, ICY name to
                                    // widget
                                }
                                if (tag.toLowerCase().startsWith(ICY_URL)) {
                                    // set message from station, URL to widget
                                }
                                tags = tags.asPointer(length + 1);
                            }
                        }
                        else {
                            // clear message from station
                        }
                    }
                    else {
                        System.out.print("progress: " + progress + " %");
                    }
                    getMetadata();
                    BASS_ChannelSetSync(chan.asInt(), BASS_SYNC_META, 0, metaSync, null);
                    BASS_ChannelSetSync(chan.asInt(), BASS_SYNC_OGG_CHANGE, 0, metaSync, null);
                    BASS_ChannelSetSync(chan.asInt(), BASS_SYNC_END, 0, endSync, null);
                    BASS_ChannelPlay(chan.asInt(), false);
                    setChannelMute(chan.asInt(), muteState);
                }

            }));

    private DOWNLOADPROC statusProc = new DOWNLOADPROC() {
        @Override
        public void DOWNLOADPROC(ByteBuffer buffer, int length, Pointer user) {

        }
    };

    private SYNCPROC metaSync = new SYNCPROC() {
        @Override
        public void SYNCPROC(HSYNC handle, int channel, int data, Pointer user) {
            getMetadata();
        }
    };

    private SYNCPROC endSync = new SYNCPROC() {
        @Override
        public void SYNCPROC(HSYNC handle, int channel, int data, Pointer user) {
            // update to message from station: stream stopped
        }
    };

    private void getMetadata() {
        Pointer metaData = BASS_ChannelGetTags(chan.asInt(), BASS_TAG_META);
        if (metaData != null) { // StreamTitle='xxx';StreamUrl='xxx';
            String meta = metaData.asString();
            if (null != meta) {
                String[] strings = meta.split(";");
                Optional<String> nowPlaying = Arrays.asList(strings).stream().filter(m -> m.startsWith("StreamTitle="))
                        .map(m -> m.replace("StreamTitle=", "")).map(m -> m.replace("'", "")).findFirst();
                if (nowPlaying.isPresent()) {
                    System.out.println(nowPlaying.get());
                }
            }
        }
        else {
            metaData = BASS_ChannelGetTags(chan.asInt(), BASS_TAG_OGG);
            if (null != metaData) {
                String artist = "", title = "";
                String meta = metaData.asString();
                if (null != meta) {
                    while (metaData.asString().length() > 0) {
                        String str = metaData.asString();
                        if (null != str) {
                            if (str.toLowerCase().startsWith("artist=")) {
                                artist = str.substring(7);
                            }
                            else if (str.toLowerCase().startsWith("title=")) {
                                title = str.substring(6);
                            }
                        }
                        else {
                            break;
                        }
                        metaData = metaData.asPointer(metaData.asString().length() + 1);
                    }
                    String streamMeta = "";
                    if (!artist.isEmpty() && !title.isEmpty()) {
                        streamMeta += artist + " - " + title;
                    }
                    else if (!title.isEmpty()) {
                        streamMeta = title;
                    }
                    System.out.println(streamMeta);
                }
            }
        }
    }

    public void setMute(boolean muteState) {
        this.muteState = muteState;
        if (null != chan) {
            setChannelMute(chan.asInt(), muteState);
        }
    }

    private void setChannelMute(int channel, boolean state) {
        if (state) {

            BASS_ChannelSetAttribute(channel, BASS_ATTRIB_VOL, 0);
        }
        else {
            BASS_ChannelSetAttribute(channel, BASS_ATTRIB_VOL, 1);
        }

    }
}
