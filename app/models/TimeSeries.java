package models;

import com.opencsv.CSVReader;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.mvc.Http;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by pulasthiiu on 10/14/15.
 */
@Entity
public class TimeSeries extends Model {

    @Id
    public Long id;

    @Constraints.Required
    @Column(unique = true)
    public String name;

    @Constraints.Required
    public String description;

    @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date dateCreation;

    @Constraints.Required
    @Formats.NonEmpty
    public Long uploaderId;

    public static Model.Finder<Long, TimeSeries> find = new Model.Finder<Long, TimeSeries>(Long.class, TimeSeries.class);

    public static TimeSeries create(String name, String description, User uploader) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.name = name;
        timeSeries.description = description;
        timeSeries.uploaderId = uploader.id;
        timeSeries.dateCreation = new Date();

        timeSeries.save();

        return timeSeries;
    }


    public static TimeSeries createFromFiles(String name, String description, User uploader, List<Http.MultipartFormData.FilePart> fileParts) throws IOException {
        TimeSeries timeSeries = create(name, description, uploader);
        String resultSetName =  "";
        for (int i = 0; i < fileParts.size(); i++) {
            File file = fileParts.get(i).getFile();
            resultSetName = "timeseries_" + name + "_" + i;
            ResultSet.createFromFile(resultSetName, description, uploader, file, timeSeries, (long)i);

        }
        return timeSeries;
    }

    public static TimeSeries findById(Long id) {
        return find.byId(id);
    }

    public static TimeSeries findByName(String name) {
        return find.where().eq("name", name).findUnique();
    }


    public static List<TimeSeries> all() {
        return find.all();
    }

}
