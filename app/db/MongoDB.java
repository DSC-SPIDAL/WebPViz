package db;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import models.Cluster;
import models.Color;
import models.ResultSet;
import models.TimeSeries;
import models.xml.PVizPoint;
import models.xml.Plotviz;
import models.xml.XMLLoader;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;
import play.Logger;

import java.io.*;
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
        Config conf = ConfigFactory.load();
        String mongoHost = conf.getString("mongo.host");
        int mongoPort = conf.getInt("mongo.port");

        MongoClient mongoClient;
        if (mongoHost != null) {
            Logger.info("Using mongo DB " + mongoHost + ":" + mongoPort);
            mongoClient = new MongoClient(mongoHost, mongoPort);
        } else {
            Logger.info("Using local mongo DB " + "localhost:27017");
            mongoClient = new MongoClient("localhost", 27017);
        }

        MongoDatabase db = mongoClient.getDatabase("pviz");

        filesCollection = db.getCollection("files");
        clustersCollection = db.getCollection("clusters");
    }

    public void insertSingleFile(String pvizName, String description, int uploader, File fileName) throws Exception {
        String dateString = format.format(new Date());
        int timeSeriesId = Math.abs(new Random().nextInt());
        Document mainDoc = new Document();
        mainDoc.append("id", timeSeriesId);
        mainDoc.append("_id", timeSeriesId);
        mainDoc.append("name", pvizName);
        mainDoc.append("desc", description);
        mainDoc.append("uploaded", uploader);
        mainDoc.append("dateCreation", dateString);
        mainDoc.append("status", "active");

        List<Document> resultSets = new ArrayList<Document>();

        String resultSetName = "timeseries_" + pvizName + "_" + 0;
        insertXMLFile(0, resultSetName, description, uploader, new FileInputStream(fileName), timeSeriesId, 0L, pvizName);
        Document resultSet = createResultSet(0, resultSetName, description, dateString, uploader, timeSeriesId, 0, pvizName);
        resultSets.add(resultSet);

        mainDoc.append("resultsets", resultSets);
        filesCollection.insertOne(mainDoc);
    }

    public boolean deleteTimeSeries(int timeSeriesId) {
        filesCollection.deleteOne(new Document("id", timeSeriesId));
        clustersCollection.deleteMany(new Document("timeSeriesId", timeSeriesId));
        return true;
    }

    public void insertZipFile(String pvizName, String description, int uploader, File fileName) throws Exception {



        String dateString = format.format(new Date());

        int timeSeriesId = Math.abs(new Random().nextInt());
        Document mainDoc = new Document();
        mainDoc.append("id", timeSeriesId);
        mainDoc.append("_id", timeSeriesId);
        mainDoc.append("name", pvizName);
        mainDoc.append("desc", description);
        mainDoc.append("uploaded", uploader);
        mainDoc.append("dateCreation", dateString);
        mainDoc.append("status", "pending");
        List<Document> emptyResultSets = new ArrayList<Document>();
        mainDoc.append("resultsets", emptyResultSets);
        filesCollection.insertOne(mainDoc);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZipFile zipFile = new ZipFile(fileName);
                    Enumeration<?> enu = zipFile.entries();
                    List<String> filesInOrder = new ArrayList<String>();
                    Map<String, ZipEntry> fileMap = new HashMap<String, ZipEntry>();
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

                    int i = 0;
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
                    zipFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mainDoc.append("status", "active");

                filesCollection.replaceOne(new Document("id", timeSeriesId), mainDoc);
            }
        });
        t.start();
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
            c.put("clusterid", cl.getKey());
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

    public String queryFile(int tid, int fid) {
        Document query = new Document("id", fid).append("timeSeriesId", tid);
        FindIterable<Document> iterable = clustersCollection.find(query);
        for (Document d : iterable) {
            return JSON.serialize(d);
        }
        return null;
    }

    public List<Cluster> getClusters(int tid, int fid) {
        Document query = new Document("id", fid).append("timeSeriesId", tid);
        FindIterable<Document> iterable = clustersCollection.find(query);
        List<Cluster> clusters = new ArrayList<Cluster>();
        for (Document d : iterable) {
            Object clusterObjects = d.get("clusters");
            if (clusterObjects instanceof List) {
                for (Object c : (List)clusterObjects) {
                    Document clusterDocument = (Document) c;
                    Cluster cluster = new Cluster();
                    cluster.resultSet = fid;
                    cluster.id = (Integer) clusterDocument.get("id");
                    cluster.cluster = (Integer) clusterDocument.get("clusterid");
                    cluster.shape = (String) clusterDocument.get("shape");
                    cluster.visible = (int) clusterDocument.get("visible");
                    cluster.size = (int) clusterDocument.get("size");
                    cluster.label = (String) clusterDocument.get("label");
                    cluster.color = createColor((Document) clusterDocument.get("color"));
                    Logger.info(JSON.serialize(clusterDocument));
                    clusters.add(cluster);
                }
            }
        }
        return clusters;
    }

    private Color createColor(Document document) {
        Color color = new Color();
        color.a = (int) document.get("a");
        color.b = (int) document.get("b");
        color.g = (int) document.get("g");
        color.r = (int) document.get("r");
        return color;
    }

    public ResultSet queryResultSetProsById(int timeSeriesId) {
        Document query = new Document("id", timeSeriesId);

        FindIterable<Document> iterable = filesCollection.find(query);
        for (Document document : iterable) {
            Object resultSetsObject = document.get("resultsets");
            if (resultSetsObject instanceof List) {
                for (Object documentObject : (List)resultSetsObject) {
                    Document resultDocument = (Document) documentObject;
                    int fId = (Integer) resultDocument.get("id");
                    ResultSet resultSet = new ResultSet();
                    try {
                        resultSet.dateCreation = format.parse((String) resultDocument.get("dateCreation"));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    resultSet.id = (Integer) resultDocument.get("id");
                    resultSet.name = (String) resultDocument.get("name");
                    resultSet.description = (String) resultDocument.get("description");
                    resultSet.uploaderId = (Integer) resultDocument.get("uploaderId");
                    resultSet.fileName = (String) resultDocument.get("fileName");
                    resultSet.timeSeriesSeqNumber = (Integer) resultDocument.get("timeSeriesSeqNumber");
                    resultSet.timeSeriesId = (Integer) resultDocument.get("timeSeriesId");
                    return resultSet;
                }
            }
        }
        return null;
    }

    public ResultSet queryResultSetProsById(int timeSeriesId, int fileId) {
        Document query = new Document("id", timeSeriesId);

        FindIterable<Document> iterable = filesCollection.find(query);
        for (Document document : iterable) {
            Object resultSetsObject = document.get("resultsets");
            if (resultSetsObject instanceof List) {
                for (Object documentObject : (List)resultSetsObject) {
                    Document resultDocument = (Document) documentObject;
                    int fId = (Integer) resultDocument.get("id");
                    if (fId == fileId) {
                        ResultSet resultSet = new ResultSet();
                        try {
                            resultSet.dateCreation = format.parse((String) resultDocument.get("dateCreation"));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        resultSet.id = (Integer) resultDocument.get("id");
                        resultSet.name = (String) resultDocument.get("name");
                        resultSet.description = (String) resultDocument.get("description");
                        resultSet.uploaderId = (Integer) resultDocument.get("uploaderId");
                        resultSet.fileName = (String) resultDocument.get("fileName");
                        resultSet.timeSeriesSeqNumber = (Integer) resultDocument.get("timeSeriesSeqNumber");
                        resultSet.timeSeriesId = (Integer) resultDocument.get("timeSeriesId");
                        return resultSet;
                    }
                }
            }
        }
        return null;
    }

    public List<ResultSet> getAllResultSet() {
        FindIterable<Document> iterable = filesCollection.find();
        List<ResultSet> resultSetList = new ArrayList<ResultSet>();
        for (Document document : iterable) {
            Object resultSetsObject = document.get("resultsets");
            if (resultSetsObject instanceof List) {
                for (Object documentObject : (List)resultSetsObject) {
                    Document resultDocument = (Document) documentObject;
                    ResultSet resultSet = new ResultSet();
                    try {
                        resultSet.dateCreation = format.parse((String) resultDocument.get("dateCreation"));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    resultSet.id = (Integer) resultDocument.get("id");
                    resultSet.name = (String) resultDocument.get("name");
                    resultSet.description = (String) resultDocument.get("description");
                    resultSet.uploaderId = (Integer) resultDocument.get("uploaderId");
                    resultSet.fileName = (String) resultDocument.get("fileName");
                    resultSet.timeSeriesSeqNumber = (Integer) resultDocument.get("timeSeriesSeqNumber");
                    resultSet.timeSeriesId = (Integer) resultDocument.get("timeSeriesId");
                    resultSetList.add(resultSet);
                }
            }
        }
        return resultSetList;
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
            timeSeries.status = (String) document.get("status");
            Object resultSetsObject = document.get("resultsets");
            if (resultSetsObject != null && resultSetsObject instanceof List) {
                if (((List)resultSetsObject).size() > 1) {
                    timeSeries.typeString = "T";
                } else {
                    timeSeries.typeString = "S";
                }
            } else {
                timeSeries.typeString = "S";
            }
            timeSeriesList.add(timeSeries);
        }
        return timeSeriesList;
    }

    public TimeSeries queryTimeSeriesProperties(int id) {
        FindIterable<Document> iterable = filesCollection.find(new Document("id", id));
        for (Document d : iterable) {
            TimeSeries timeSeries = new TimeSeries();
            timeSeries.id = (Integer) d.get("id");
            timeSeries.name = (String) d.get("name");
            return timeSeries;
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
