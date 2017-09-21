package player.view;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import player.PlayerMain;

public class PlayerMainController {

    private PlayerMain playerMain;

    public void setPlayerMain(PlayerMain playerMain) {
        this.playerMain = playerMain;
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
            playerMain.setStreamToFile(file);
        }
    }

    @FXML
    private void handlePlay() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Play stream");
        alert.setHeaderText("NetRadioPlayer");
        alert.setContentText("This button starts the stream.");
        alert.showAndWait();
    }

    @FXML
    private void handleStop() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Stop stream");
        alert.setHeaderText("NetRadioPlayer");
        alert.setContentText("This button stops the stream.");
        alert.showAndWait();
    }
}
