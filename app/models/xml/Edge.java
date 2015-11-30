package models.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

public class Edge {
    private String key;
    private List<Vertex> vertices;

    public String getKey() {
        return key;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    @XmlElement
    public void setKey(String key) {
        this.key = key;
    }

    @XmlElementWrapper(name = "vertices")
    @XmlElement(name="vertex")
    public void setVertices(List<Vertex> vertices) {
        this.vertices = vertices;
    }
}
