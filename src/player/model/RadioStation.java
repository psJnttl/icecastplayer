package player.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class RadioStation {

    private StringProperty name;
    private StringProperty stationUrl;

    public RadioStation() {
        this(null,null);  // needed for JAXB
    }

    public RadioStation(String name, String stationUrl) {
        super();
        this.name = new SimpleStringProperty(name);
        this.stationUrl = new SimpleStringProperty(stationUrl);
    }

    public StringProperty getNameProperty() {
        return name;
    }

    public String getName() {
        return this.name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty getStationUrlProperty() {
        return stationUrl;
    }

    public String getStationUrl() {
        return this.stationUrl.get();
    }

    public void setStationUrl(String stationUrl) {
        this.stationUrl.set(stationUrl);
    }

}
