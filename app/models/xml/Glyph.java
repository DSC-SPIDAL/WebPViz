package models.xml;


import javax.xml.bind.annotation.XmlElement;

public class Glyph {
    private int visible = 1;
    private int scale = 1;

    public Glyph() {
    }

    public Glyph(int visible, int scale) {
        this.visible = visible;
        this.scale = scale;
    }

    public int getVisible() {
        return visible;
    }

    public int getScale() {
        return scale;
    }

    @XmlElement
    public void setVisible(int visible) {
        this.visible = visible;
    }

    @XmlElement
    public void setScale(int scale) {
        this.scale = scale;
    }
}
