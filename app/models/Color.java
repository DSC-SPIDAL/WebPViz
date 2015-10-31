package models;

import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Color extends Model {
    @Id
    public Long id;

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
