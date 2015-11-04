package db;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import models.TimeSeries;
import models.xml.PVizPoint;
import models.xml.Plotviz;
import models.xml.XMLLoader;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;
import play.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MongoDB {
    MongoCollection<Document> filesCollection;
    MongoCollection<Document> clustersCollection;

    public static MongoDB db = new MongoDB();

    public static MongoDB getInstance() {
        return db;
    }
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private MongoDB() {
        MongoClient mongoClient = new MongoClient("localhost", 27017);

        MongoDatabase db = mongoClient.getDatabase("pviz");

        filesCollection = db.getCollection("files");
        clustersCollection = db.getCollection("clusters");
    }

    public void insertZipFile(String pvizName, String description, int uploader, File fileName) throws Exception {
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


        String dateString = format.format(new Date());

        int timeSeriesId = Math.abs(new Random().nextInt());
        Document mainDoc = new Document();
        mainDoc.append("id", timeSeriesId);
        mainDoc.append("_id", timeSeriesId);
        mainDoc.append("name", pvizName);
        mainDoc.append("desc", description);
        mainDoc.append("uploaded", uploader);
        mainDoc.append("dateCreation", dateString);

        List<Document> resultSets = new ArrayList<Document>();

        for (String f : filesInOrder) {
            if (fileMap.get(f) != null) {
                String resultSetName = "timeseries_" + f + "_" + i;
                insertXMLFile(i, resultSetName, description, uploader, zipFile.getInputStream(fileMap.get(f)), timeSeriesId, (long) i, f);
                Document resultSet = createResultSet(i, resultSetName, description, dateString, uploader, timeSeriesId, i, f);
                resultSets.add(resultSet);
                i++;
            }
        }

        mainDoc.append("resultsets", resultSets);
        filesCollection.insertOne(mainDoc);

        zipFile.close();
    }

    public Document createResultSet(int id, String name, String description, String dateCreation, int uploaderId, int timeSeriesId, int timeSeriesSeqNumber, String originalFileName) {
        Document document = new Document();
        document.append("id", id).append("name", name).append("description", description).
                append("dateCreation", dateCreation).append("uploaderId", uploaderId).
                append("timeSeriesId", timeSeriesId).append("timeSeriesSeqNumber", timeSeriesSeqNumber).append("fileName", originalFileName);
        return document;
    }

    public void insertXMLFile(int id, String name, String description, int uploader, InputStream file,
                              int parent, Long sequenceNumber, String originalFileName) throws Exception {
        Document clustersDbObject = new Document();
        clustersDbObject.append("id", id);
        clustersDbObject.append("name", name);
        clustersDbObject.append("desc", description);
        clustersDbObject.append("uploaded", uploader);
        clustersDbObject.append("fileName",  originalFileName);
        clustersDbObject.append("timeSeriesId", parent);
        clustersDbObject.append("timeSeriesSeqNumber", sequenceNumber);

        Plotviz plotviz = XMLLoader.load(file);
        List<models.xml.Cluster> clusters = plotviz.getClusters();
        Map<Integer, Document> clusterDBObjects = new HashMap<Integer, Document>();
        for (models.xml.Cluster cl : clusters) {
            Document c = new Document();
            c.put("color", new Document().append("a", cl.getColor().getA()).append("b", cl.getColor().getB()).append("g", cl.getColor().getG()).append("r", cl.getColor().getR()));
            c.put("label", cl.getLabel());
            c.put("size", cl.getSize());
            c.put("visible", cl.getVisible());
            c.put("shape", cl.getShape());
            clusterDBObjects.put(cl.getKey(), c);
        }

        List<PVizPoint> points = plotviz.getPoints();
        Map<Integer, List<Document>> pointsForClusters = new HashMap<Integer, List<Document>>();
        for (int i = 0; i < points.size(); i++) {
            PVizPoint point = points.get(i);
            int clusterkey = point.getClusterkey();

            List<Document> basicDBObjectList = pointsForClusters.get(clusterkey);
            if (basicDBObjectList == null) {
                basicDBObjectList = new ArrayList<Document>();
                pointsForClusters.put(clusterkey, basicDBObjectList);
            }
            Document pointDBObject = createPoint(point.getLocation().getX(), point.getLocation().getY(), point.getLocation().getZ(), clusterkey);
            basicDBObjectList.add(pointDBObject);
        }

        Iterator<Map.Entry<Integer, List<Document>>> entries = pointsForClusters.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, List<Document>> e = entries.next();
            if (e.getValue() != null && e.getValue().size() > 0) {
                Document clusterDBObject = clusterDBObjects.get(e.getKey());
                clusterDBObject.append("points", e.getValue());
            } else {
                Logger.info("Remove: " + e.getKey());
                entries.remove();
            }
        }

        for(Iterator<Map.Entry<Integer, Document>> it = clusterDBObjects.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Document> entry = it.next();
            if (!pointsForClusters.containsKey(entry.getKey())) {
                it.remove();
            }
        }

        // add each cluster to clusters object
        List<Document> clustersList = new ArrayList<Document>(clusterDBObjects.values());
        clustersDbObject.append("clusters", clustersList);

        clustersCollection.insertOne(clustersDbObject);
    }

    public Document createFile(int id, String name, String description, String dateCreated, int uploadedId, int timeSeriesId, int timeSeriesSeqNumber, String fileName) {
        Document object = new Document();
        object.append("id", id);
        object.append("name", name);
        object.append("description", description);
        object.append("dateCreation", dateCreated);
        object.append("uploaderId", uploadedId);
        object.append("timeSeriesId", timeSeriesId);
        object.append("timeSeriesSeqNumber", timeSeriesSeqNumber);
        object.append("fileName", fileName);

        return object;
    }

    public Document createPoint( Float x, Float y, Float z, int cluster){
        Document object = new Document();
        object.append("x", x);
        object.append("y", y);
        object.append("z", z);
        object.append("cluster", cluster);

        return object;
    }

    public String queryTimeSeriesAll(int id) {
        FindIterable<Document> iterable = clustersCollection.find(new Document("id", id));
        for (Document d : iterable) {
            return JSON.serialize(d);
        }
        return null;
    }

    public String queryTimeSeries(int id) {
        Document query = new Document("id", id);
        FindIterable<Document> iterable = filesCollection.find(query);
        for (Document d : iterable) {
            return JSON.serialize(d);
        }
        return null;
    }

    public List<TimeSeries> getAllTimeSeries() {
        FindIterable<Document> iterable = filesCollection.find();
        List<TimeSeries> timeSeriesList = new ArrayList<TimeSeries>();
        for (Document document : iterable) {
            TimeSeries timeSeries = new TimeSeries();
            try {
                timeSeries.dateCreation = format.parse((String) document.get("dateCreation"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            timeSeries.id = (Integer) document.get("id");
            timeSeries.name = (String) document.get("name");
            timeSeries.description = (String) document.get("desc");
            timeSeries.uploaderId = (Integer) document.get("uploaded");
            timeSeriesList.add(timeSeries);
        }
        return timeSeriesList;
    }

    public Map<String, Object> queryTimeSeriesProperties(int id) {
        FindIterable<Document> iterable = filesCollection.find(new Document("id", id));
        for (Document d : iterable) {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("id", d.get("id"));
            props.put("name", d.get("name"));
            return props;
        }
        return null;
    }

    public static void main(String[] args) {
        MongoDB mongoDB = new MongoDB();

        try {
            mongoDB.insertZipFile("aaa", "aaa", 1, new File("/home/supun/data/OCT_14/upload.zip"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
