package models.xml;

import javax.xml.bind.annotation.XmlAttribute;

public class Location {
    private String x;
    private String y;
    private String z;

    public Location(String x, String y, String z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location() {
    }

    @XmlAttribute
    public void setX(String x) {
        this.x = x;
    }

    @XmlAttribute
    public void setY(String y) {
        this.y = y;
    }

    @XmlAttribute
    public void setZ(String z) {
        this.z = z;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getZ() {
        return z;
    }
}
