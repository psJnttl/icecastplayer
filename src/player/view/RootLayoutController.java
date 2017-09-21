package player.view;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import player.PlayerMain;

public class RootLayoutController {

    private PlayerMain playerMain;
    
    public void setPlayerMain(PlayerMain playerMain) {
        this.playerMain = playerMain;
    }
    
    @FXML
    private void handleExit() {
        playerMain.shutDown();
        System.exit(0);
    }
    
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("NetRadioPlayer");
        alert.setContentText("Streaming mp3 and ogg streams with ease.");
        alert.showAndWait();
    }
}
