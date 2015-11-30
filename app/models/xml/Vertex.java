package models.xml;

import javax.xml.bind.annotation.XmlAttribute;

public class Vertex {
    private String key;

    public String getKey() {
        return key;
    }

    @XmlAttribute
    public void setKey(String key) {
        this.key = key;
    }
}
