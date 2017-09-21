package player.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "radiostations")
public class StationListWrapper {

    private List<RadioStation> stationList;

    @XmlElement(name = "radiostation")
    public List<RadioStation> getStationList() {
        return stationList;
    }

    public void setStationList(List<RadioStation> stationList) {
        this.stationList = stationList;
    }

}
