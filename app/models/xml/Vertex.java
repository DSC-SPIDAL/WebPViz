package models.xml;

import javax.xml.bind.annotation.XmlAttribute;

public class Vertex {
    private int key;

    public int getKey() {
        return key;
    }

    @XmlAttribute
    public void setKey(int key) {
        this.key = key;
    }
}
