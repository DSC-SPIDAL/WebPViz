package models.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder={"key","clusterkey","label", "location"})
public class PVizPoint {
    private int key;
    private int clusterkey;
    private String label;
    private Location location;

    public PVizPoint(int key, int clusterKey, String label, Location location) {
        this.key = key;
        this.clusterkey = clusterKey;
        this.label = label;
        this.location = location;
    }

    public PVizPoint() {
    }

    public int getKey() {
        return key;
    }

    public int getClusterkey() {
        return clusterkey;
    }

    public String getLabel() {
        return label;
    }

    public Location getLocation() {
        return location;
    }

    @XmlElement
    public void setKey(int key) {
        this.key = key;
    }

    @XmlElement
    public void setClusterkey(int clusterkey) {
        this.clusterkey = clusterkey;
    }

    @XmlElement
    public void setLabel(String label) {
        this.label = label;
    }

    @XmlElement
    public void setLocation(Location location) {
        this.location = location;
    }
}
