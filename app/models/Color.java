package models;

import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;

public class Color extends Model {
    @Id
    public Integer id;

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
