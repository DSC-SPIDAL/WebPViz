package models;

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.*;

@Entity
public class TimeSeries extends Model {
    @Id
    public Integer id;

    @Constraints.Required
    @Column(unique = true)
    public String name;

    @Constraints.Required
    public String description;

    @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date dateCreation;

    @Constraints.Required
    @Formats.NonEmpty
    public Integer uploaderId;

    public String status;

    public String typeString = "T";

    public static Model.Finder<Integer, TimeSeries> find = new Model.Finder<Integer, TimeSeries>(Integer.class, TimeSeries.class);

    public static List<TimeSeries> all() {
        return find.all();
    }
}
