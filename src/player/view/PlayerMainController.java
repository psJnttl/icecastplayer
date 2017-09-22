package player.view;

import java.io.File;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import player.PlayerMain;
import player.model.RadioStation;

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

    public void setStationSelectList(ObservableList<RadioStation> stationList) {
        stationSelectList.clear();
        stationSelectList.addAll(stationList);
    }

    public void initialize() {
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
            this.streamUrlField.setText(station.getStationUrl());
        });

    }

    public void setPlayerMain(PlayerMain playerMain) {
        this.playerMain = playerMain;
        streamToFilenameField.textProperty().addListener((observable, oldValue, newValue) -> {
            playerMain.setStreamToFile(newValue);
        });
        streamUrlField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("StreamUrl: " + newValue);
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
        File file = fileChooser.showOpenDialog(playerMain.getPrimaryStage());
        if (null != file) {
            playerMain.loadStationList(file);
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
        File file = fileChooser.showSaveDialog(playerMain.getPrimaryStage());

        if (null != file) {
            streamToFilenameField.setText(file.getAbsolutePath());
            playerMain.setStreamToFile(file);
        }
    }

    @FXML
    private void handlePlay() {
        playerMain.playStream(this.selectedStation.getStationUrl());
    }

    @FXML
    private void handleStop() {
        playerMain.stopStream();
    }

    public void setDisableFileSelection(boolean state) {
        streamToFilenameField.setDisable(state);
        saveToButton.setDisable(state);
    }
}
