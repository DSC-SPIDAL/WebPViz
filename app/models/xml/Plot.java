package models.xml;

import javax.xml.bind.annotation.XmlElement;

public class Plot {
    private String title;
    private int pointsize;

    private Glyph glyph;

    public String getTitle() {
        return title;
    }

    public int getPointsize() {
        return pointsize;
    }

    public Glyph getGlyph() {
        return glyph;
    }

    @XmlElement
    public void setTitle(String title) {
        this.title = title;
    }

    @XmlElement
    public void setPointsize(int pointsize) {
        this.pointsize = pointsize;
    }

    @XmlElement
    public void setGlyph(Glyph glyph) {
        this.glyph = glyph;
    }
}
