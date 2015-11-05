package models;

import com.opencsv.CSVReader;
import org.apache.commons.io.FilenameUtils;
import play.Logger;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.mvc.Http;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    public static Model.Finder<Integer, TimeSeries> find = new Model.Finder<Integer, TimeSeries>(Integer.class, TimeSeries.class);

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
            String originalFileName = fileParts.get(i).getFilename();
            resultSetName = "timeseries_" + name + "_" + i;
            ResultSet.createFromFile(resultSetName, description, uploader, file, timeSeries, i, originalFileName);
        }
        return timeSeries;
    }

    /**
     * Create the timeseries from the zip file. The zip file should include a text file with the extension pviz,
     * that contains the file names in the time series order.
     * @param pvizName name of the upload
     * @param description description provided
     * @param uploader user
     * @param fileName the file
     * @return the created TimeSeries
     * @throws IOException
     */
    public static TimeSeries createFromZip(String pvizName, String description, User uploader, File fileName) throws Exception {
        TimeSeries timeSeries = create(pvizName, description, uploader);
        ZipFile zipFile = new ZipFile(fileName);
        Enumeration<?> enu = zipFile.entries();
        List<String> filesInOrder = new ArrayList<String>();
        Map<String, ZipEntry> fileMap = new HashMap<String, ZipEntry>();
        int i = 0;
        while (enu.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enu.nextElement();
            String name = zipEntry.getName();
            String ext = FilenameUtils.getExtension(name);
            String realFileName = FilenameUtils.getName(name);

            File file = new File(name);
            if (name.endsWith("/")) {
                file.mkdirs();
                continue;
            }

            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            if (ext != null && ext.equals("index")) {
                BufferedReader bufRead = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry)));
                String inputLine;
                while ((inputLine = bufRead.readLine()) != null) {
                    filesInOrder.add(inputLine);
                }
                continue;
            }

            fileMap.put(realFileName, zipEntry);
        }

        for (String f : filesInOrder) {
            if (fileMap.get(f) != null) {
                String resultSetName = "timeseries_" + f + "_" + i;
                Logger.info("Processing XML File: " + fileMap.get(f));
                ResultSet.createFromXMLFile(resultSetName, description, uploader, zipFile.getInputStream(fileMap.get(f)), timeSeries, i, f);
                i++;
            }
        }
        zipFile.close();
        return timeSeries;
    }

    public static TimeSeries findById(Integer id) {
        return find.byId(id);
    }

    public static TimeSeries findByName(String name) {
        return find.where().eq("name", name).findUnique();
    }


    public static List<TimeSeries> all() {
        return find.all();
    }

}
