package player.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class AlertDialog {
    private AlertType alertType;
    private String title;
    private String header;
    private String content;

    public static class Builder {
        private AlertType alertType = AlertType.ERROR;
        private String title = "Error";
        private String header = "Generic error";
        private String content = "";

        public Builder alertType(AlertType alertType) {
            this.alertType = alertType;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder header(String header) {
            this.header = header;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public AlertDialog build() {
            return new AlertDialog(this);
        }
    }

    private AlertDialog(Builder builder) {
        this.alertType = builder.alertType;
        this.title = builder.title;
        this.header = builder.header;
        this.content = builder.content;
    }

    public void show() {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
