package db;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBList;
import com.mongodb.client.FindIterable;
import com.mongodb.util.JSON;
import models.*;
import models.Cluster;
import models.Color;
import models.xml.*;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;
import play.Logger;
import scala.util.parsing.json.JSONArray;
import scala.util.parsing.json.JSONArray$;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArtifactDAO {
    private static ArtifactDAO db = new ArtifactDAO();

    public static ArtifactDAO getInstance() {
        return db;
    }

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Insert a single fie pviz file or a txt file
     * @param pvizName name of the uploaded file
     * @param description description of the file
     * @param uploader the uploader name
     * @param file the actual file
     * @throws Exception  if the file cannot be inserted
     */
    public void insertSingleFile(String pvizName, String description, String uploader, File file, String group) throws Exception {
        MongoConnection con = MongoConnection.getInstance();
        String dateString = format.format(new Date());
        int timeSeriesId = Math.abs(new Random().nextInt());
        Document mainDoc = new Document();
        mainDoc.append(Constants.Artifact.ID_FIELD, timeSeriesId);
        mainDoc.append(Constants.Artifact.NAME_FIELD, pvizName);
        mainDoc.append(Constants.Artifact.DESC_FIELD, description);
        mainDoc.append(Constants.Artifact.USER, uploader);
        mainDoc.append(Constants.Artifact.DATE_CREATION_FIELD, dateString);
        mainDoc.append(Constants.Artifact.STATUS_FIELD, "active");
        mainDoc.append(Constants.Artifact.GROUP_FIELD, group);
        mainDoc.append(Constants.Artifact.VERSION, 1);
        mainDoc.append(Constants.Artifact.TYPE, Constants.ArtifactType.PLOTVIZ);


        String resultSetName = pvizName + "/";
        // insert the file content to the files collection
        // lets try to load as a XML
        try {
            XMLLoader.load(new FileInputStream(file));
            // ok this is XML
            insertXMLFile(0, resultSetName, description, uploader, new FileInputStream(file), timeSeriesId, 0L, pvizName);
        } catch (Exception e) {
            // this is not XML
            insertTextFile(0, resultSetName, description, uploader, new FileInputStream(file), timeSeriesId, 0L, pvizName);
        }

        Document resultSet = createResultSet(0, resultSetName, description, dateString, uploader, timeSeriesId, 0, pvizName);
        List<Document> emptyResultSets = new ArrayList<Document>();
        emptyResultSets.add(resultSet);
        mainDoc.append(Constants.Artifact.FILES, emptyResultSets);
        con.artifactCol.insertOne(mainDoc);
    }

    private List<PVizPoint> getPointsFromTextFile(InputStream file) {
        List<PVizPoint> points = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file))) {
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\s+");
                if (split.length == 5) {
                    PVizPoint point = new PVizPoint(i++, 1, "None", new Location(split[1], split[2], split[3]));
                    points.add(point);
                } else if (split.length == 3){
                    PVizPoint point = new PVizPoint(i++, 1, "None", new Location(split[0], split[1], split[2]));
                    points.add(point);
                } else if(split.length == 6){
                    PVizPoint point = new PVizPoint(i++, Integer.valueOf(split[4]), split[5], new Location(split[1], split[2], split[3]));
                    points.add(point);
                }
            }
        } catch (IOException e) {
            Logger.error("Failed to read the file", e);
        }
        return points;
    }

  /**
   * Insert a text file. Text files doesn't have clusters or edges. They only have points.
   * @param id id
   * @param name name
   * @param description description
   * @param uploader uploader
   * @param file file
   * @param parent parent
   * @param sequenceNumber sequence number
   * @param originalFileName original file name
   * @throws Exception
   */
    private void insertTextFile(int id, String name, String description, String uploader, InputStream file,
                                int parent, Long sequenceNumber, String originalFileName) throws Exception {
        MongoConnection con = MongoConnection.getInstance();
        // maximum number of points per file
        final int maxPointsPerFile = 100000;
        Map<Integer, Integer> clusterPointCount = new HashMap<>();
        Document rootObject = createRootFileObject(id, name, description, uploader, parent, sequenceNumber, originalFileName);

        // traverse through the clusters and create the cluster list
        Map<Integer, Document> clusterDBObjectList = new HashMap<Integer, Document>();
//        Document c = new Document();
//        c.put(Constants.Cluster.KEY, 1);
//        c.put(Constants.Cluster.LABEL, "Default");
//        c.put(Constants.Cluster.SIZE, 1);
//        c.put(Constants.Cluster.VISIBILE, 1);

        // now traverse through the points and create the point list
        List<PVizPoint> points = getPointsFromTextFile(file);
        // point key for each cluster
        Map<Integer, List<Integer>> pointsForClusters = new HashMap<Integer, List<Integer>>();
        Map<String, List<String>> pointList = new HashMap<>();
        int tempcount = 0;
        for (PVizPoint point : points) {

            tempcount++;
            int clusterkey = point.getClusterkey();
            if(!clusterDBObjectList.containsKey(clusterkey)){
                Document tempcluster = new Document();
                tempcluster.put(Constants.Cluster.KEY, clusterkey);
                if(point.getLabel() != null && point.getLabel() != "None" && point.getLabel() != "" && point.getLabel() != " "){
                    tempcluster.put(Constants.Cluster.LABEL, point.getLabel());
                }else{
                    tempcluster.put(Constants.Cluster.LABEL, clusterkey);
                }

                tempcluster.put(Constants.Cluster.SIZE, 1);
                tempcluster.put(Constants.Cluster.VISIBILE, 1);
                tempcluster.put(Constants.Cluster.SHAPE, 3);
                tempcluster.put(Constants.Cluster.COLOR, color(255,255,255,255));
                clusterDBObjectList.put(clusterkey, tempcluster);
            }

            int pointKey = point.getKey();
            List<Integer> clusterPoints = pointsForClusters.get(clusterkey);
            if (clusterPoints == null) {
                clusterPoints = new ArrayList<Integer>();
                pointsForClusters.put(clusterkey, clusterPoints);
            }
            List<String> pointDBObject = createPoint(point.getLocation().getX(), point.getLocation().getY(), point.getLocation().getZ(),point.getLabel());
            // add the key to cluster and point to point list
            clusterPoints.add(pointKey);
            pointList.put(Integer.toString(pointKey), pointDBObject);
        }

        Iterator<Map.Entry<Integer, List<Integer>>> entries = pointsForClusters.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, List<Integer>> e = entries.next();
            if (e.getValue() != null && e.getValue().size() > 0) {
                Document clusterDBObject = clusterDBObjectList.get(e.getKey());
                clusterDBObject.append(Constants.Cluster.POINTS, e.getValue());
                clusterPointCount.put(e.getKey(), e.getValue().size());
            } else {
                Logger.info("Remove: " + e.getKey());
                entries.remove();
            }
        }

        // remove the clusters without any points
        for(Iterator<Map.Entry<Integer, Document>> it = clusterDBObjectList.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Document> entry = it.next();
            if (!pointsForClusters.containsKey(entry.getKey())) {
                it.remove();
            }
        }

        // add the point key list to clusters
        for (Map.Entry<Integer, List<Integer>> e : pointsForClusters.entrySet()) {
            Document clusterDocument = clusterDBObjectList.get(e.getKey());
            clusterDocument.append(Constants.Cluster.POINTS, e.getValue());
        }

        // add each cluster to clusters object
        // we are going to create separate docs when total number of points exceeds
        int count = 0;
        Map<String, Document> currentClusterList = new HashMap<>();
        for (Map.Entry<Integer, Document> e : clusterDBObjectList.entrySet()) {
            count += clusterPointCount.get(e.getKey());
            currentClusterList.put(Integer.toString(e.getKey()), e.getValue());
            if (count > maxPointsPerFile) {
                count = 0;
                Document preRootObject = createRootFileObject(id, name, description, uploader, parent, sequenceNumber, originalFileName);
                preRootObject.append(Constants.File.CLUSTERS, currentClusterList);
                con.filesCol.insertOne(preRootObject);
                currentClusterList = new HashMap<>();
                Logger.info("Breaking file clusters: " + originalFileName);
            }
        }
        // we will add the remainder or the whole list, if we didn't exceed the max number
        if (currentClusterList.size() > 0) {
            rootObject.append(Constants.File.CLUSTERS, currentClusterList);
        }

        count = 0;
        Map<String, List<String>> currentPointList = new HashMap<>();
        for (Map.Entry<String, List<String>> e : pointList.entrySet()) {
            currentPointList.put(e.getKey(), e.getValue());
            count++;
            if (count > maxPointsPerFile) {
                count = 0;
                Document preRootObject = createRootFileObject(id, name, description, uploader, parent, sequenceNumber, originalFileName);
                preRootObject.append(Constants.File.POINTS, currentPointList);
                con.filesCol.insertOne(preRootObject);
                currentPointList = new HashMap<>();
                Logger.info("Breaking file points: " + originalFileName);
            }
        }
        // we will add the remainder or the whole list, if we didn't exceed the max number
        if (currentPointList.size() > 0) {
            rootObject.append(Constants.File.POINTS, currentPointList);
        }

        con.filesCol.insertOne(rootObject);
        Logger.info("Inserted document: " + originalFileName);
    }

    /**
     * Delete the time series files
     * @param timeSeriesId delete the file
     * @return true if delete successful
     */
    public boolean deleteTimeSeries(int timeSeriesId, String user) {
        MongoConnection con = MongoConnection.getInstance();
        con.artifactCol.deleteOne(new Document(Constants.Artifact.ID_FIELD, timeSeriesId).append(Constants.Artifact.USER, user));
        con.filesCol.deleteMany(new Document(Constants.File.TIME_SERIES_ID_FIELD, timeSeriesId));
        return true;
    }

    /**
     * Insert a zip file containing the time series files
     * @param pvizName name of the uploaded plotviz file
     * @param description description
     * @param uploader the uploader id
     * @param fileName file name
     * @throws Exception if an error happens while inserting
     */
    public void insertZipFile(String pvizName, String description, String uploader, File fileName, String group) throws Exception {
        MongoConnection con = MongoConnection.getInstance();
        String dateString = format.format(new Date());
        int timeSeriesId = Math.abs(new Random().nextInt());
        Document mainDoc = new Document();
        mainDoc.append(Constants.Artifact.ID_FIELD, timeSeriesId);
        mainDoc.append("_id", timeSeriesId);
        mainDoc.append(Constants.Artifact.NAME_FIELD, pvizName);
        mainDoc.append(Constants.Artifact.DESC_FIELD, description);
        mainDoc.append(Constants.Artifact.USER, uploader);
        mainDoc.append(Constants.Artifact.DATE_CREATION_FIELD, dateString);
        mainDoc.append(Constants.Artifact.STATUS_FIELD, Constants.ArtifactStatus.PENDING);
        List<Document> emptyResultSets = new ArrayList<Document>();
        mainDoc.append(Constants.Artifact.FILES, emptyResultSets);
        mainDoc.append(Constants.Artifact.GROUP_FIELD, group);
        mainDoc.append(Constants.Artifact.TYPE, Constants.ArtifactType.TIME_SERIES);
        con.artifactCol.insertOne(mainDoc);

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
                            Logger.info("adding file: " + f);
                            String resultSetName = "timeseries_" + f + "_" + i;
                            insertXMLFile(i, resultSetName, description, uploader, zipFile.getInputStream(fileMap.get(f)), timeSeriesId, (long) i, f);
                            Document resultSet = createResultSet(i, resultSetName, description, dateString, uploader, timeSeriesId, i, f);
                            resultSets.add(resultSet);
                            i++;

                        }
                    }
                    mainDoc.append(Constants.Artifact.FILES, resultSets);
                    zipFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mainDoc.append(Constants.Artifact.STATUS_FIELD, Constants.ArtifactStatus.ACTIVE);

                con.artifactCol.replaceOne(new Document(Constants.Artifact.ID_FIELD, timeSeriesId), mainDoc);
            }
        });
        t.start();
    }

    public void updateArtifactSetting(TimeSeries tid, String json) {
        MongoConnection con = MongoConnection.getInstance();
        Object data = JSON.parse(json);
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Artifact.ID_FIELD, tid.id).append(Constants.Artifact.USER, tid.uploaderId);

        FindIterable<Document> iterable = con.artifactCol.find(oldGroupDocument);
        Document findDocument = null;
        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument != null) {
            if (findDocument.containsKey(Constants.Artifact.SETTINGS)) {
                findDocument.replace(Constants.Artifact.SETTINGS, data);
            } else {
                findDocument.append(Constants.Artifact.SETTINGS, data);
            }
            con.artifactCol.replaceOne(oldGroupDocument, findDocument);
        }
    }

    public Document createResultSet(int id, String name, String description, String dateCreation, String uploaderId, int timeSeriesId, int timeSeriesSeqNumber, String originalFileName) {
        Document document = new Document();
        document.append(Constants.Artifact.ID_FIELD, id).append(Constants.Artifact.NAME_FIELD, name).append(Constants.Artifact.DESCRIPTION_FIELD, description).
                append(Constants.Artifact.DATE_CREATION_FIELD, dateCreation).append(Constants.UPLOADER_ID_FIELD, uploaderId).
                append(Constants.File.TIME_SERIES_ID_FIELD, timeSeriesId).append(Constants.File.TIME_SERIES_SEQ_NUMBER_FIELD, timeSeriesSeqNumber).
                append(Constants.File.FILE_NAME_FIELD, originalFileName);
        return document;
    }

    private BasicDBList color(int a, int r, int g, int b) {
        BasicDBList list = new BasicDBList();
        list.add(a);
        list.add(r);
        list.add(g);
        list.add(b);
        return list;
    }

    public void insertXMLFile(int id, String name, String description, String uploader, InputStream file,
                              int parent, Long sequenceNumber, String originalFileName) throws Exception {
        MongoConnection con = MongoConnection.getInstance();
        // maximum number of points per file
        final int maxPointsPerFile = 100000;
        Map<Integer, Integer> clusterPointCount = new HashMap<>();
        Document rootObject = createRootFileObject(id, name, description, uploader, parent, sequenceNumber, originalFileName);

        Plotviz plotviz = XMLLoader.load(file);

        // traverse through the clusters and create the cluster list
        List<models.xml.Cluster> clusters = plotviz.getClusters();
        Map<Integer, Document> clusterDBObjectList = new HashMap<Integer, Document>();
        for (models.xml.Cluster cl : clusters) {
            Document c = new Document();
            c.put(Constants.Cluster.KEY, cl.getKey());
            if (cl.getColor() != null) {
                c.put(Constants.Cluster.COLOR, color(cl.getColor().getA(), cl.getColor().getB(), cl.getColor().getG(), cl.getColor().getR()));
            }
            c.put(Constants.Cluster.LABEL, cl.getLabel());
            c.put(Constants.Cluster.SIZE, cl.getSize());
            c.put(Constants.Cluster.VISIBILE, cl.getVisible());
            c.put(Constants.Cluster.SHAPE, cl.getShape());
            clusterDBObjectList.put(cl.getKey(), c);
        }

        // now traverse through the points and create the point list
        List<PVizPoint> points = plotviz.getPoints();
        // point key for each cluster
        Map<Integer, List<Integer>> pointsForClusters = new HashMap<Integer, List<Integer>>();
        Map<String, List<String>> pointList = new HashMap<>();
        for (PVizPoint point : points) {
            int clusterkey = point.getClusterkey();
            int pointKey = point.getKey();
            List<Integer> clusterPoints = pointsForClusters.get(clusterkey);
            if (clusterPoints == null) {
                clusterPoints = new ArrayList<Integer>();
                pointsForClusters.put(clusterkey, clusterPoints);
            }
            List<String> pointDBObject = createPoint(point.getLocation().getX(), point.getLocation().getY(), point.getLocation().getZ(),point.getLabel());
            // add the key to cluster and point to point list
            clusterPoints.add(pointKey);
            pointList.put(Integer.toString(pointKey), pointDBObject);
        }

        Iterator<Map.Entry<Integer, List<Integer>>> entries = pointsForClusters.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, List<Integer>> e = entries.next();
            if (e.getValue() != null && e.getValue().size() > 0) {
                Document clusterDBObject = clusterDBObjectList.get(e.getKey());
                clusterDBObject.append(Constants.Cluster.POINTS, e.getValue());
                clusterPointCount.put(e.getKey(), e.getValue().size());
            } else {
                Logger.info("Remove: " + e.getKey());
                entries.remove();
            }
        }

        // remove the clusters without any points
        for(Iterator<Map.Entry<Integer, Document>> it = clusterDBObjectList.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Document> entry = it.next();
            if (!pointsForClusters.containsKey(entry.getKey())) {
                it.remove();
            }
        }

        // add the point key list to clusters
        for (Map.Entry<Integer, List<Integer>> e : pointsForClusters.entrySet()) {
            Document clusterDocument = clusterDBObjectList.get(e.getKey());
            clusterDocument.append(Constants.Cluster.POINTS, e.getValue());
        }

        // clusters to root object
        // rootObject.append(Constants.File.CLUSTERS, clusterDBObjectList.values());
        // add points to root object list
        // rootObject.append(Constants.File.POINTS, pointList);

        // add each cluster to clusters object
        // we are going to create separate docs when total number of points exceeds
        int count = 0;
        Map<String, Document> currentClusterList = new HashMap<>();
        for (Map.Entry<Integer, Document> e : clusterDBObjectList.entrySet()) {
            count += clusterPointCount.get(e.getKey());
            currentClusterList.put(Integer.toString(e.getKey()), e.getValue());
            if (count > maxPointsPerFile) {
                count = 0;
                Document preRootObject = createRootFileObject(id, name, description, uploader, parent, sequenceNumber, originalFileName);
                preRootObject.append(Constants.File.CLUSTERS, currentClusterList);
                con.filesCol.insertOne(preRootObject);
                currentClusterList = new HashMap<>();
                Logger.info("Breaking file clusters: " + originalFileName);
            }
        }
        // we will add the remainder or the whole list, if we didn't exceed the max number
        if (currentClusterList.size() > 0) {
            rootObject.append(Constants.File.CLUSTERS, currentClusterList);
        }

        count = 0;
        Map<String, List<String>> currentPointList = new HashMap<>();
        for (Map.Entry<String, List<String>> e : pointList.entrySet()) {
            currentPointList.put(e.getKey(), e.getValue());
            count++;
            if (count > maxPointsPerFile) {
                count = 0;
                Document preRootObject = createRootFileObject(id, name, description, uploader, parent, sequenceNumber, originalFileName);
                preRootObject.append(Constants.File.POINTS, currentPointList);
                con.filesCol.insertOne(preRootObject);
                currentPointList = new HashMap<>();
                Logger.info("Breaking file points: " + originalFileName);
            }
        }
        // we will add the remainder or the whole list, if we didn't exceed the max number
        if (currentPointList.size() > 0) {
            rootObject.append(Constants.File.POINTS, currentPointList);
        }

        // now insert the edges if there are any
        Map<String, Document> edgesList = new HashMap<>();
        List<Edge> edges = plotviz.getEdges();
        if (edges != null && edges.size() > 0) {
            for (Edge e : edges) {
                Document edgeDoc = new Document();
                List<Vertex> vertexes = e.getVertices();
                List<Integer> vertices = new ArrayList<>();
                if (vertexes != null && vertexes.size() > 0) {
                    List<Document> vertexDocs = new ArrayList<Document>();
                    for (Vertex v : vertexes) {
                        vertices.add(v.getKey());
                    }
                    edgeDoc.append(Constants.Edge.VERTICES, vertices);
                } else {
                    // no point adding this edge, because it doesn't have vertices
                    continue;
                }
                edgesList.put(Integer.toString(e.getKey()), edgeDoc);
            }
            rootObject.append(Constants.File.EDGES, edgesList);
        }
        con.filesCol.insertOne(rootObject);
        Logger.info("Inserted document: " + originalFileName);
    }

    private Document pointsDocument(Map<String, List<Float>> points) {
        Document pointsDocument = new Document();
        for (Map.Entry<String, List<Float>> e : points.entrySet()) {
            pointsDocument.append(e.getKey(), e.getValue());
        }
        return pointsDocument;
    }

    /**
     * Constructs the root cluster object
     * @param id id
     * @param name name
     * @param description description
     * @param uploader user
     * @param parent the big file
     * @param sequenceNumber sequence
     * @param originalFileName original file
     * @return document
     */
    private Document createRootFileObject(int id, String name, String description, String uploader, int parent, Long sequenceNumber, String originalFileName) {
        Document rootObject = new Document();
        rootObject.append(Constants.Artifact.ID_FIELD, id);
        rootObject.append(Constants.Artifact.NAME_FIELD, name);
        rootObject.append(Constants.Artifact.DESC_FIELD, description);
        rootObject.append(Constants.Artifact.USER, uploader);
        rootObject.append(Constants.File.FILE_NAME_FIELD, originalFileName);
        rootObject.append(Constants.File.TIME_SERIES_ID_FIELD, parent);
        rootObject.append(Constants.File.TIME_SERIES_SEQ_NUMBER_FIELD, sequenceNumber);
        return rootObject;
    }

    /**
     * Create a point document
     * @param x
     * @param y
     * @param z
     * @return
     */
    public List<String> createPoint(String x, String y, String z, String label) {
        List<String> list = new ArrayList<>();
        list.add(x);
        list.add(y);
        list.add(z);
        list.add(label);
        return list;
    }

    public String getArtifact(int id, String user) {
        MongoConnection con = MongoConnection.getInstance();
        Document query;

        if (user != null) {
            query = new Document(Constants.Artifact.ID_FIELD, id).append(Constants.Artifact.USER, user);
        } else {
            query = new Document(Constants.Artifact.ID_FIELD, id).append(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE);
        }

        FindIterable<Document> iterable = con.artifactCol.find(query);
        for (Document d : iterable) {
            return JSON.serialize(d);
        }

        return null;
    }

    public String getFile(int tid, int fid, String user) {
        MongoConnection con = MongoConnection.getInstance();
        // if there is no user specified the file has to be public
        if (user != null) {
            Document tidQuery = new Document(Constants.Artifact.ID_FIELD, tid).append(Constants.Artifact.USER, user);
            FindIterable<Document> iterable = con.artifactCol.find(tidQuery);
            boolean found = false;
            for (Document d : iterable) {
                found = true;
            }
            if (!found) {
                return null;
            }
        } else {
            Document tidQuery = new Document(Constants.Artifact.ID_FIELD, tid).append(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE);
            FindIterable<Document> iterable = con.artifactCol.find(tidQuery);
            boolean found = false;
            for (Document d : iterable) {
                found = true;
            }
            if (!found) {
                return null;
            }
        }

        Document query = new Document(Constants.Artifact.ID_FIELD, fid).append(Constants.File.TIME_SERIES_ID_FIELD, tid);
        FindIterable<Document> iterable = con.filesCol.find(query);
        Document mainDoc = new Document();
        for (Document d : iterable) {
            mainDoc = d;
            break;
        }

        Map<String, Object> clusters = new HashMap<>();
        Map<String, Object> edges = new HashMap<>();
        Map<String, Object> points = new HashMap<>();
        Map<String, Object> stats = new HashMap<>();
        for (Document d : iterable) {
            Object clusterObjects = d.get(Constants.File.CLUSTERS);
            if (clusterObjects instanceof Document) {
                clusters.putAll((Document)clusterObjects);
            }

            Object edgeObjects = d.get(Constants.File.EDGES);
            if (edgeObjects instanceof Document) {
                edges.putAll((Document)edgeObjects);
            }
            Object pointObjects = d.get(Constants.File.POINTS);
            if (pointObjects instanceof Document) {
                points.putAll((Document) pointObjects);
            }
        }
        if (mainDoc.containsKey(Constants.File.CLUSTERS)) {
            mainDoc.replace(Constants.File.CLUSTERS, clusters);
        } else {
            mainDoc.append(Constants.File.CLUSTERS, clusters);
        }
        if (edges.size() > 0) {
            mainDoc.replace(Constants.File.EDGES, edges);
        }
        if (mainDoc.containsKey(Constants.File.POINTS)) {
            mainDoc.replace(Constants.File.POINTS, points);
        } else {
            mainDoc.append(Constants.File.POINTS, points);
        }

        //Calculate Stats and append
        if(fid == 0){
            double[] means = new double[3];
            int count = 0;
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = null;

            for (Object point : points.values()) {
                jsonArray = (JsonArray) jsonParser.parse(point.toString());
                means[0] += jsonArray.get(0).getAsDouble();
                means[1] += jsonArray.get(1).getAsDouble();
                means[2] += jsonArray.get(2).getAsDouble();
                count++;
            }

            means[0] = means[0]/count;
            means[1] = means[1]/count;
            means[2] = means[2]/count;

            stats.put("means",means);
            mainDoc.append(Constants.File.STATS,stats);
        }
        String serialize = JSON.serialize(mainDoc);
        Logger.info("Retreived document with tid: " + tid + " fid: " + fid);
        return serialize;
    }

    public List<Cluster> clusters(int tid, int fid) {
        MongoConnection con = MongoConnection.getInstance();
        Document query = new Document(Constants.Artifact.ID_FIELD, fid).append(Constants.File.TIME_SERIES_ID_FIELD, tid);
        FindIterable<Document> iterable = con.filesCol.find(query);
        List<Cluster> clusters = new ArrayList<Cluster>();
        for (Document d : iterable) {
            Object clusterObjects = d.get(Constants.File.CLUSTERS);
            if (clusterObjects instanceof List) {
                for (Object c : (List)clusterObjects) {
                    Document clusterDocument = (Document) c;
                    Cluster cluster = new Cluster();
                    cluster.resultSet = fid;
                    cluster.id = (Integer) clusterDocument.get(Constants.Cluster.KEY);
                    cluster.cluster = (Integer) clusterDocument.get(Constants.Cluster.KEY);
                    cluster.shape = (String) clusterDocument.get(Constants.Cluster.SHAPE);
                    cluster.visible = (int) clusterDocument.get(Constants.Cluster.VISIBILE);
                    cluster.size = (int) clusterDocument.get(Constants.Cluster.SIZE);
                    cluster.label = (String) clusterDocument.get(Constants.Cluster.LABEL);
                    Object colorObject = clusterDocument.get(Constants.Cluster.COLOR);
                    if (colorObject != null) {
                        cluster.color = color(colorObject);
                    }
                    clusters.add(cluster);
                }
            }
        }
        return clusters;
    }

    private Color color(Object document) {
        if (document instanceof List && ((List) document).size() >= 4) {
            List colors = (List) document;
            return new Color((Integer)colors.get(0), (Integer)colors.get(1), (Integer)colors.get(2), (Integer)colors.get(3));
        }
        return null;
    }

//    public ResultSet individualFile(int id, String user) {
//        MongoConnection con = MongoConnection.getInstance();
//        FindIterable<Document> iterable;
//        if (user != null) {
//            iterable = con.artifactCol.find(new Document(Constants.Artifact.ID_FIELD, id).append(Constants.Artifact.USER, user));
//        } else {
//            iterable = con.artifactCol.find(new Document(Constants.Artifact.ID_FIELD, id).append(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE));
//        }
//        for (Document d : iterable) {
//            ResultSet timeSeries = new ResultSet();
//            timeSeries.id = (Integer) d.get(Constants.Artifact.ID_FIELD);
//            timeSeries.name = (String) d.get(Constants.Artifact.NAME_FIELD);
//            timeSeries.description = (String) d.get(Constants.Artifact.DESCRIPTION_FIELD);
//            timeSeries.uploaderId = (String) d.get(Constants.Artifact.USER);
//            return timeSeries;
//        }
//        return null;
//    }


    public ResultSet individualFile(int timeSeriesId, String user) {
        MongoConnection con = MongoConnection.getInstance();
        Document query = new Document(Constants.Artifact.ID_FIELD, timeSeriesId);
        FindIterable<Document> iterable;
        if (user != null) {
            iterable = con.artifactCol.find(query.append(Constants.Artifact.USER, user));
        } else {
            iterable = con.artifactCol.find(query.append(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE));
        }
        for (Document document : iterable) {
            Object resultSetsObject = document.get(Constants.Artifact.FILES);
            if (resultSetsObject instanceof List) {
                for (Object documentObject : (List)resultSetsObject) {
                    Document resultDocument = (Document) documentObject;
                    int fId = (Integer) resultDocument.get(Constants.Artifact.ID_FIELD);
                    ResultSet resultSet = new ResultSet();
                    try {
                        resultSet.dateCreation = format.parse((String) resultDocument.get(Constants.Artifact.DATE_CREATION_FIELD));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    resultSet.id = (Integer) resultDocument.get(Constants.Artifact.ID_FIELD);
                    resultSet.name = (String) resultDocument.get(Constants.Artifact.NAME_FIELD);
                    resultSet.description = (String) resultDocument.get(Constants.Artifact.DESCRIPTION_FIELD);
                    Object userId = resultDocument.get(Constants.UPLOADER_ID_FIELD);
                    if (userId instanceof String) {
                        resultSet.uploaderId = userId.toString();
                    }
                    resultSet.fileName = (String) resultDocument.get(Constants.File.FILE_NAME_FIELD);
                    resultSet.timeSeriesSeqNumber = 0;
                    resultSet.timeSeriesId = timeSeriesId;
                    return resultSet;
                }
            }
        }
        return null;
    }

    public List<ResultSet> individualFiles() {
        MongoConnection con = MongoConnection.getInstance();
        FindIterable<Document> iterable = con.artifactCol.find();
        List<ResultSet> resultSetList = new ArrayList<ResultSet>();
        for (Document document : iterable) {
            Object resultSetsObject = document.get(Constants.Artifact.FILES);
            if (resultSetsObject instanceof List) {
                for (Object documentObject : (List)resultSetsObject) {
                    Document resultDocument = (Document) documentObject;
                    ResultSet resultSet = new ResultSet();
                    try {
                        resultSet.dateCreation = format.parse((String) resultDocument.get(Constants.Artifact.DATE_CREATION_FIELD));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    resultSet.id = (Integer) resultDocument.get(Constants.Artifact.ID_FIELD);
                    resultSet.name = (String) resultDocument.get(Constants.Artifact.NAME_FIELD);
                    resultSet.description = (String) resultDocument.get(Constants.Artifact.DESCRIPTION_FIELD);
//                    resultSet.uploaderId = (String) resultDocument.get(Constants.UPLOADER_ID_FIELD);
                    resultSet.fileName = (String) resultDocument.get(Constants.File.FILE_NAME_FIELD);
                    resultSet.timeSeriesSeqNumber = (Integer) resultDocument.get(Constants.File.TIME_SERIES_SEQ_NUMBER_FIELD);
                    resultSet.timeSeriesId = (Integer) resultDocument.get(Constants.File.TIME_SERIES_ID_FIELD);
                    resultSetList.add(resultSet);
                }
            }
        }
        return resultSetList;
    }

    /**
     * Get the uploaded entity list
     *
     * @return uploaded entity list
     */
    public List<TimeSeries> timeSeriesList(String user) {
        MongoConnection con = MongoConnection.getInstance();
        FindIterable<Document> iterable;
        if (user != null) {
            iterable = con.artifactCol.find(new Document(Constants.Artifact.USER, user));
        } else {
            iterable = con.artifactCol.find(new Document(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE));
        }
        return getTimeSeriesList(iterable);
    }

    public List<TimeSeries> timeSeriesList(Group group, String user) {
        MongoConnection con = MongoConnection.getInstance();
        Document findDoc = new Document();
        findDoc.append(Constants.Artifact.GROUP_FIELD, group.name);
        if (user != null) {
            findDoc.append(Constants.Artifact.USER, user);
        } else {
            findDoc.append(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE);
        }
        FindIterable<Document> iterable = con.artifactCol.find(findDoc);
        return getTimeSeriesList(iterable);
    }

    private List<TimeSeries> getTimeSeriesList(FindIterable<Document> iterable) {
        List<TimeSeries> timeSeriesList = new ArrayList<TimeSeries>();
        for (Document document : iterable) {
            TimeSeries timeSeries = new TimeSeries();
            try {
                timeSeries.dateCreation = format.parse((String) document.get(Constants.Artifact.DATE_CREATION_FIELD));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            timeSeries.id = (Integer) document.get(Constants.Artifact.ID_FIELD);
            timeSeries.name = (String) document.get(Constants.Artifact.NAME_FIELD);
            timeSeries.description = (String) document.get(Constants.Artifact.DESC_FIELD);
            timeSeries.uploaderId = (String) document.get(Constants.Artifact.USER);
            timeSeries.status = (String) document.get(Constants.Artifact.STATUS_FIELD);
            timeSeries.group = (String) document.get(Constants.Artifact.GROUP_FIELD);
            Object pub = document.get(Constants.Artifact.PUBLIC);
            if (pub != null && pub instanceof Boolean) {
                timeSeries.pub = (boolean) pub;
            }
            if (timeSeries.group == null || "".equals(timeSeries.group)) {
                timeSeries.group = Constants.Group.DEFAULT_GROUP;
            }
            Object resultSetsObject = document.get(Constants.Artifact.FILES);
            if (resultSetsObject != null && resultSetsObject instanceof List) {
                if (((List)resultSetsObject).size() > 1) {
                    timeSeries.t = "T";
                } else {
                    timeSeries.t = "S";
                }
            } else {
                timeSeries.t = "S";
            }
            timeSeriesList.add(timeSeries);
        }
        return timeSeriesList;
    }

    public boolean timeSeriesExists(TimeSeries timeSeries) {
        MongoConnection con = MongoConnection.getInstance();
        Document doc = new Document();
        doc.append(Constants.Artifact.ID_FIELD, timeSeries.id).append(Constants.Artifact.USER, timeSeries.uploaderId);
        FindIterable<Document> iterable = con.artifactCol.find(doc);
        return iterable.iterator().hasNext();
    }

    public void updateTimeSeries(TimeSeries old, TimeSeries newTimeSeries) {
        MongoConnection con = MongoConnection.getInstance();
        Logger.info("updating the document with id " + old.id + " with group: " + newTimeSeries.group + " desc: " + newTimeSeries.description);
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Artifact.ID_FIELD, old.id).append(Constants.Artifact.USER, old.uploaderId);

        FindIterable<Document> iterable = con.artifactCol.find(oldGroupDocument);
        Document findDocument = null;
        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument != null) {
            findDocument.append(Constants.Artifact.NAME_FIELD, newTimeSeries.name);
            findDocument.append(Constants.Artifact.GROUP_FIELD, newTimeSeries.group);
            findDocument.append(Constants.Artifact.DESC_FIELD, newTimeSeries.description);
            findDocument.append(Constants.Artifact.PUBLIC, newTimeSeries.pub);
            con.artifactCol.replaceOne(oldGroupDocument, findDocument);
        }
    }

    public TimeSeries timeSeries(int id, String user) {
        MongoConnection con = MongoConnection.getInstance();
        FindIterable<Document> iterable;
        if (user != null) {
            iterable = con.artifactCol.find(new Document(Constants.Artifact.ID_FIELD, id).append(Constants.Artifact.USER, user));
        } else {
            iterable = con.artifactCol.find(new Document(Constants.Artifact.ID_FIELD, id).append(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE));
        }
        for (Document d : iterable) {
            TimeSeries timeSeries = new TimeSeries();
            timeSeries.id = (Integer) d.get(Constants.Artifact.ID_FIELD);
            timeSeries.name = (String) d.get(Constants.Artifact.NAME_FIELD);
            Object resultSetsObject = d.get(Constants.Artifact.FILES);
            try {
                timeSeries.dateCreation = format.parse((String) d.get(Constants.Artifact.DATE_CREATION_FIELD));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (resultSetsObject != null && resultSetsObject instanceof List) {
                if (((List)resultSetsObject).size() > 1) {
                    timeSeries.t = "T";
                } else {
                    timeSeries.t = "S";
                }
            } else {
                timeSeries.t = "S";
            }
            timeSeries.group = (String) d.get(Constants.Artifact.GROUP_FIELD);
            timeSeries.description = (String) d.get(Constants.Artifact.DESC_FIELD);
            timeSeries.uploaderId = (String) d.get(Constants.Artifact.USER);
            Object pub = d.get(Constants.Artifact.PUBLIC);
            if (pub != null && pub instanceof Boolean) {
                timeSeries.pub = (boolean) pub;
            }
            return timeSeries;
        }
        return null;
    }
}
