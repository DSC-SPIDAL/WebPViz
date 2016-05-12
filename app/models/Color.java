package models;

import com.avaje.ebean.Model;

public class Color extends Model {
    public int a;
    public int b;
    public int g;
    public int r;

    public Color() {
    }

    public Color(int a, int b, int g, int r) {
        this.a = a;
        this.b = b;
        this.g = g;
        this.r = r;
    }
}
