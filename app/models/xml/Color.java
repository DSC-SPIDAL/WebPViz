package models.xml;

import javax.xml.bind.annotation.XmlAttribute;

public class Color {
    private int r;
    private int g;
    private int b;
    private int a;

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public int getA() {
        return a;
    }

    @XmlAttribute
    public void setR(int r) {
        this.r = r;
    }

    @XmlAttribute
    public void setG(int g) {
        this.g = g;
    }

    @XmlAttribute
    public void setB(int b) {
        this.b = b;
    }

    @XmlAttribute
    public void setA(int a) {
        this.a = a;
    }
}
