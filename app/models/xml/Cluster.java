package models.xml;

import javax.xml.bind.annotation.XmlElement;

public class Cluster {
    private int key;
    private String label;
    private int visible;
    private int defaultValue;
    private Color color;
    private int size;
    private String shape;

    public String getShape() {
        return shape;
    }

    @XmlElement
    public void setShape(String shape) {
        this.shape = shape;
    }

    @XmlElement
    public void setKey(int key) {
        this.key = key;
    }

    @XmlElement
    public void setLabel(String label) {
        this.label = label;
    }

    @XmlElement
    public void setVisible(int visible) {
        this.visible = visible;
    }

    @XmlElement (name = "default")
    public void setDefaultValue(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    @XmlElement
    public void setColor(Color color) {
        this.color = color;
    }

    @XmlElement
    public void setSize(int size) {
        this.size = size;
    }

    public int getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public int getVisible() {
        return visible;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public Color getColor() {
        return color;
    }

    public int getSize() {
        return size;
    }
}
