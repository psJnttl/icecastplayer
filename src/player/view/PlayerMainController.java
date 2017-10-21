package player.view;

import java.io.File;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import player.PlayerMain;
import player.model.RadioStation;
import player.util.AlertDialog;

public class PlayerMainController {

    private PlayerMain playerMain;

    @FXML
    private ComboBox<RadioStation> stationSelect;
    private ObservableList<RadioStation> stationSelectList = FXCollections.observableArrayList();
    private RadioStation selectedStation;
    @FXML
    private Button saveToButton;
    @FXML
    private TextField streamUrlField;
    @FXML
    private TextField streamToFilenameField;
    @FXML
    private CheckBox muteCheckBox;
    @FXML
    private Label streamTitle;
    @FXML
    private Label stationId;
    private Preferences preferences;
    private static final String XML_DIR = "xmlDir";
    private static final String STREAM_DIR = "streamDir";

    public void setStationSelectList(ObservableList<RadioStation> stationList) {
        stationSelect.getSelectionModel().clearSelection();
        this.streamUrlField.setText("");
        stationSelectList.clear();
        stationSelectList.addAll(stationList);
    }

    public void initialize() {
        preferences = Preferences.userNodeForPackage(PlayerMainController.class);
        stationSelect.setItems(stationSelectList);
        stationSelect.setCellFactory((select) -> { // rendering options
            return new ListCell<RadioStation>() {
                protected void updateItem(RadioStation item, boolean empty) {
                    super.updateItem(item, empty);
                    if (null == item || empty) {
                        setText(null);
                    }
                    else {
                        setText(item.getName());
                    }
                }
            };
        });
        stationSelect.setConverter(new StringConverter<RadioStation>() {
            @Override
            public String toString(RadioStation station) {
                if (null != station) {
                    return station.getName();
                }
                return null;
            }

            @Override
            public RadioStation fromString(String string) {
                return null;
            }
        });
        stationSelect.setOnAction((event) -> {
            RadioStation station = stationSelect.getSelectionModel().getSelectedItem();
            this.selectedStation = station;
            if (null != station) {
                this.streamUrlField.setText(station.getStationUrl());
            }
        });

    }

    public void setPlayerMain(PlayerMain playerMain) {
        this.playerMain = playerMain;
        streamToFilenameField.textProperty().addListener((observable, oldValue, newValue) -> {
            playerMain.setStreamToFile(newValue);
        });
        streamUrlField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (null != selectedStation) {
                selectedStation.setStationUrl(newValue);
            }
            else {
                selectedStation = new RadioStation("manual", newValue);
            }
        });
    }

    @FXML
    private void handleStreamFrom() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(xmlFilter);
        String stationDir = preferences.get(XML_DIR, "");
        if (!stationDir.isEmpty()) {
            fileChooser.setInitialDirectory(new File(stationDir));
        }
        File file = fileChooser.showOpenDialog(playerMain.getPrimaryStage());
        if (null != file) {
            playerMain.loadStationList(file);
            stationDir = file.getAbsolutePath();
            stationDir = stationDir.substring(0, stationDir.lastIndexOf("\\"));
            preferences.put(XML_DIR, stationDir);
        }
    }

    @FXML
    private void handleSaveTo() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter mp3Filter = new FileChooser.ExtensionFilter("MP3 files (*.mp3)", "*.mp3");
        FileChooser.ExtensionFilter oggFilter = new FileChooser.ExtensionFilter("OGG files (*.ogg)", "*.ogg");
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("all files (*.*)", "*.*");
        fileChooser.getExtensionFilters().add(mp3Filter);
        fileChooser.getExtensionFilters().add(oggFilter);
        fileChooser.getExtensionFilters().add(allFilter);
        String streamDir = preferences.get(STREAM_DIR, "");
        if (!streamDir.isEmpty()) {
            fileChooser.setInitialDirectory(new File(streamDir));
        }
        File file = fileChooser.showSaveDialog(playerMain.getPrimaryStage());

        if (null != file) {
            streamToFilenameField.setText(file.getAbsolutePath());
            playerMain.setStreamToFile(file);
            streamDir = file.getAbsolutePath();
            streamDir = streamDir.substring(0, streamDir.lastIndexOf("\\"));
            preferences.put(STREAM_DIR, streamDir);
        }
    }

    @FXML
    private void handlePlay() {
        if (null != selectedStation) {
            playerMain.playStream(selectedStation.getStationUrl());
        }
    }

    @FXML
    private void handleStop() {
        playerMain.stopStream();
    }

    public void setDisableFileSelection(boolean state) {
        streamToFilenameField.setDisable(state);
        saveToButton.setDisable(state);
    }

    @FXML
    private void handleMute() {
        boolean mute = muteCheckBox.isSelected();
        playerMain.setMute(mute);
    }

    public void updateTitle(String text) {
        if (null != text) {
            Platform.runLater(() -> {
                this.streamTitle.setText(text);
            });
        }
    }

    public void updateStationId(String text) {
        if (null != text) {
            Platform.runLater(() -> {
                this.stationId.setText(text);
            });

        }
    }
    
    public void updateAppTitle(String title) {
        Stage stage = playerMain.getPrimaryStage();
        if (null != stage && null != title && !title.isEmpty()) {
            Platform.runLater( () -> {
            stage.setTitle(title);
            });
        }
    }

    public void showAlertDialog(AlertDialog dialog) {
        if (null != dialog) {
            Platform.runLater(() -> {
                dialog.show();
            });
        }
    }
}
