package player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
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
import player.util.AlertDialog;
import player.util.Device;
import player.util.StreamFile;

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
    private PlayerMainController playerMainController;
    private Thread streamingThread = null;
    private HSTREAM chan = null;
    private boolean muteState = false;
    private StreamFile streamFile = null;
    private static final String DRAG_OK_STYLE = "-fx-border-color: green; -fx-border-style: dashed; -fx-border-width: 3";
    private static final String DRAG_FAIL_STYLE = "-fx-border-color: red; -fx-border-style: dashed; -fx-border-width: 3";

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
        this.primaryStage.setTitle("icecastplayer");
        this.primaryStage.setResizable(false);
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
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header(e.getMessage())
                    .content("Failed to load object hierarchy from FXML")
                    .build();
            playerMainController.showAlertDialog(alertDialog);
        }
    }

    private void showPlayerMain() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(PlayerMain.class.getResource("view/PlayerMainLayout.fxml"));
            AnchorPane playerMainView = (AnchorPane) loader.load();
            rootLayout.setCenter(playerMainView);
            setupDragDrop(rootLayout);
            playerMainController = loader.getController();
            playerMainController.setPlayerMain(this);
            playerMainController.setStationSelectList(stationList);
        }
        catch (IOException e) {
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header(e.getMessage())
                    .content("Failed to load object hierarchy from FXML")
                    .build();
            playerMainController.showAlertDialog(alertDialog);
        }
    }
    
    private void setupDragDrop(Pane view) {
        String styleBackup = view.getStyle();
        view.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                Dragboard dragBoard = event.getDragboard();
                Optional<String> filename  = dragBoard.getFiles().stream()
                    .filter(f -> f.getName().toLowerCase().endsWith(".xml"))
                    .map(f -> f.getAbsolutePath())
                    .findFirst();
                if (filename.isPresent()) {
                    event.acceptTransferModes(TransferMode.COPY);
                    view.setStyle(DRAG_OK_STYLE);
                }
                else {
                    view.setStyle(DRAG_FAIL_STYLE);
                }
                event.consume();
            }
        });
        view.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                boolean status = false;
                Dragboard dragBoard = event.getDragboard();
                Optional<File> xmlFile = dragBoard.getFiles().stream()
                    .filter(p -> p.getAbsolutePath().toLowerCase().endsWith(".xml"))
                    .findFirst();
                if (xmlFile.isPresent()) {
                    loadStationList(xmlFile.get());
                    status = true;
                }
                view.setStyle(styleBackup);
                event.setDropCompleted(status);
                event.consume();
            }
        });
        view.setOnDragExited(new EventHandler<DragEvent> () {

            @Override
            public void handle(DragEvent event) {
                view.setStyle(styleBackup);
                event.consume();
            } 
        });
        
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
            try {
                if (null != streamFile) {
                    streamFile.closeStreamFile();
                    streamFile = null;
                }
                if (null != streamToFileName && !streamToFileName.isEmpty()) {
                    streamFile = StreamFile.openStreamFile(streamToFileName);
                }
                openStreamUrl(stationUrl);
            }
            catch (IOException e) {
                AlertDialog alertDialog = new AlertDialog.Builder()
                        .header(e.getMessage())
                        .build();
                playerMainController.showAlertDialog(alertDialog);
                stopStream();
            }
        }
    }

    public void stopStream() {
        this.primaryStage.setTitle("icecastplayer");
        try {
            if (null != streamFile) {
                streamFile.closeStreamFile();
                streamFile = null;
            }
        }
        catch (IOException e) {
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header(e.getMessage())
                    .build();
            playerMainController.showAlertDialog(alertDialog);
        }
        playerMainController.setDisableFileSelection(false);
        if (null != chan) {
            BASS_ChannelStop(chan.asInt());
            chan = null;
        }
    }

    public void shutDown() {
        if (null != streamFile) {
            try {
                streamFile.closeStreamFile();
                streamFile = null;
            }
            catch (IOException e) {
                AlertDialog alertDialog = new AlertDialog.Builder()
                        .header(e.getMessage())
                        .build();
                playerMainController.showAlertDialog(alertDialog);
            }
        }
        closeBassNative();
    }

    public void stop() { // in case app closed by closing window
        shutDown();
    }

    public void loadStationList(File file) {
        try (FileInputStream fileStream = new FileInputStream(file)) { 
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(StationListWrapper.class);
                Unmarshaller unmarshaler = jaxbContext.createUnmarshaller();
                StationListWrapper stationListWrapper = (StationListWrapper) unmarshaler.unmarshal(fileStream);
                stationList.clear();
                stationList.addAll(stationListWrapper.getStationList());
                playerMainController.setStationSelectList(stationList);
            }
            catch (JAXBException e) {
                String message = e.getMessage();
                String b = e.toString();
                if (null != e.getLinkedException()) {
                    message = e.getLinkedException().getMessage();
                }
                AlertDialog alertDialog = new AlertDialog.Builder()
                        .header("Couldn't load station list from XML file.\n" + 
                                file.getAbsolutePath())
                        .content(message)
                        .build();
                playerMainController.showAlertDialog(alertDialog);
            }
        }
        catch (FileNotFoundException e1) {
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header("File not found!\n" + file.getAbsolutePath() )
                    .content(e1.getMessage())
                    .build();
            playerMainController.showAlertDialog(alertDialog);


        }
        catch (IOException e1) {
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header("Can't open file!\n" + file.getAbsolutePath())
                    .build();
            playerMainController.showAlertDialog(alertDialog);
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
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header("Failed to save stations to an XML file.")
                    .content(e.getCause().toString() + "\n" + e.getMessage())
                    .build();
            playerMainController.showAlertDialog(alertDialog);
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
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header("Failed to load Bass library")
                    .content(e.getMessage())
                    .build();
            playerMainController.showAlertDialog(alertDialog);
            return;
        }
        if (BassInit.NATIVEBASS_LIBRARY_VERSION() != BassInit.NATIVEBASS_JAR_VERSION()) {
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header("Library version does not match.")
                    .content("lib version: " + BassInit.NATIVEBASS_LIBRARY_VERSION() + "\n" + 
                             "JAR version: " + BassInit.NATIVEBASS_JAR_VERSION())
                    .build();
            playerMainController.showAlertDialog(alertDialog);
            return;
        }
        if (((BASS_GetVersion() & 0xFFFF0000) >> 16) != BassInit.BASSVERSION()) {
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header("An incorrect version of BASS.DLL was loaded.")
                    .content("Should be: " + ((BASS_GetVersion() & 0xFFFF0000) >> 16) + "\n" + 
                             "lib version: " + BassInit.BASSVERSION())
                    .build();
            playerMainController.showAlertDialog(alertDialog);
            return;
        }
        if (!BASS_Init(Device.forceNoSoundDevice(-1), Device.forceFrequency(44100), 0, null, null)) {
            AlertDialog alertDialog = new AlertDialog.Builder()
                    .header("Can't initialize device.")
                    .build();
            playerMainController.showAlertDialog(alertDialog);
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
        if (BassInit.isLibrariesLoaded()) {
            BASS_Free();
        }
    }

    private void openStreamUrl(String stationUrl) {
        if (null != streamingThread) {
            return;
        }
        streamingThread = new Thread() {
            public synchronized void start() {
                streamBufferTimer.stop();
                BASS_StreamFree(chan);
                chan = BASS_StreamCreateURL(stationUrl, 0,
                        BASS_STREAM_BLOCK | BASS_STREAM_STATUS | BASS_STREAM_AUTOFREE, statusProc, null);
                if (null == chan) {
                    AlertDialog alertDialg = new AlertDialog.Builder()
                            .header("Failed to start stream")
                            .content("Please check the stream URL.")
                            .build();
                    playerMainController.showAlertDialog(alertDialg);
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
                                final String ICY_BR = "icy-br:";

                                String tag = tags.asString();
                                if (tag.toLowerCase().startsWith(ICY_NAME)) {
                                    playerMainController.updateStationId(tag.substring(ICY_NAME.length()));
                                }
                                if (tag.toLowerCase().startsWith(ICY_BR)) {
                                    // to bitrate indicator
                                }
                                tags = tags.asPointer(length + 1);
                            }
                        }
                        else {
                            // clear message from station
                        }
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
            if (buffer != null && length > 0 && null != streamFile) {
                try {
                    streamFile.writeStreamData(buffer, length);
                }
                catch (IOException ex) {
                    AlertDialog alertDialg = new AlertDialog.Builder()
                            .content("Can't write stream buffer to file!")
                            .header(ex.getMessage())
                            .build();
                    playerMainController.showAlertDialog(alertDialg);
                }
            }
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
                    playerMainController.updateTitle(nowPlaying.get());
                    playerMainController.updateAppTitle(nowPlaying.get());
                    if (null != streamFile) {
                        try {
                            streamFile.writeMetaData(nowPlaying.get());
                        }
                        catch (IOException e) {
                            AlertDialog alertDialg = new AlertDialog.Builder()
                                    .content("Can't write stream meta data to file!")
                                    .header(e.getMessage())
                                    .build();
                            playerMainController.showAlertDialog(alertDialg);
                        }
                    }
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
                    playerMainController.updateTitle(streamMeta);
                    playerMainController.updateAppTitle(streamMeta);
                    if (null != streamFile) {
                        try {
                            streamFile.writeMetaData(streamMeta);
                        }
                        catch (IOException e) {
                            AlertDialog alertDialg = new AlertDialog.Builder()
                                    .content("Can't write stream meta data to file!")
                                    .header(e.getMessage())
                                    .build();
                            playerMainController.showAlertDialog(alertDialg);
                        }
                    }
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
