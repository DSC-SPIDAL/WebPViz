package models;

import javax.persistence.Entity;

@Entity
public class Color {
    public int a;
    public int b;
    public int g;
    public int r;

    public Color(int a, int b, int g, int r) {
        this.a = a;
        this.b = b;
        this.g = g;
        this.r = r;
    }
}
