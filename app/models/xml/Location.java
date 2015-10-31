package models.xml;

import javax.xml.bind.annotation.XmlAttribute;

public class Location {
    private float x;
    private float y;
    private float z;

    public Location(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location() {
    }

    @XmlAttribute
    public void setX(float x) {
        this.x = x;
    }

    @XmlAttribute
    public void setY(float y) {
        this.y = y;
    }

    @XmlAttribute
    public void setZ(float z) {
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
}
