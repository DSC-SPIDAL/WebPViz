package edu.iu.soic.dsc;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConvertPointsToString {
    public MongoCollection<Document> artifactCol;
    public MongoCollection<Document> filesCol;
    public MongoCollection<Document> groupsCol;
    NumberFormat formatter = new DecimalFormat("#0.00000");

    public static final String MONGO_HOST = "mongo.host";
    public static final String MONGO_PORT = "mongo.port";
    public static final String ARTIFACTS_COLLECTION = "artificats";
    public static final String FILES_COLLECTION = "files";
    public static final String DB_NAME = "pviz2";
    public static final String GROUPS_COLLECTION = "groups";

    private int count = 0;

    public void createConnection(String mongoHost, int mongoPort) {
        MongoClient mongoClient;
        if (mongoHost != null) {
            mongoClient = new MongoClient(mongoHost, mongoPort);
        } else {
            mongoClient = new MongoClient("localhost", 27017);
        }

        MongoDatabase db = mongoClient.getDatabase(DB_NAME);

        artifactCol = db.getCollection(ARTIFACTS_COLLECTION);
        filesCol = db.getCollection(FILES_COLLECTION);
        groupsCol = db.getCollection(GROUPS_COLLECTION);
    }

    public void changeFileUserID() {
        Iterable<Document> fileDocuments = filesCol.find();
        for (Document d : fileDocuments) {
            d.append("uploaderId", "webplotviziu");
            Document replace = new Document("_id", d.get("_id"));
//            Iterable<Document> it = artifactCol.find(replace);
//            for (Document t : it) {
//                System.out.println(t);
//            }
            UpdateResult update = filesCol.replaceOne(replace, d);
            System.out.println(update);
        }
        System.out.println("Changing groups");
        Iterable<Document> groupDocuments = groupsCol.find();
        for (Document d : groupDocuments) {
            d.append("user", "webplotviziu");
            Document replace = new Document("_id", d.get("_id"));
//            Iterable<Document> it = groupsCol.find(replace);
//            for (Document t : it) {
//                System.out.println(t);
//            }
            UpdateResult updateResult = groupsCol.replaceOne(replace, d);
            System.out.println(updateResult);
        }
    }

    public void changeUserID() {
        Iterable<Document> fileDocuments = artifactCol.find();
        for (Document d : fileDocuments) {
            d.append("uploader", "webplotviziu");
            Document replace = new Document("_id", d.get("_id"));
//            Iterable<Document> it = artifactCol.find(replace);
//            for (Document t : it) {
//                System.out.println(t);
//            }
            UpdateResult update = artifactCol.replaceOne(replace, d);
            System.out.println(update);
        }
        System.out.println("Changing groups");
        Iterable<Document> groupDocuments = groupsCol.find();
        for (Document d : groupDocuments) {
            d.append("user", "webplotviziu");
            Document replace = new Document("_id", d.get("_id"));
//            Iterable<Document> it = groupsCol.find(replace);
//            for (Document t : it) {
//                System.out.println(t);
//            }
            UpdateResult updateResult = groupsCol.replaceOne(replace, d);
            System.out.println(updateResult);
        }
    }

    public void changeFileCollection() {
        Iterable<Document> fileDocuments = filesCol.find();
        for (Document d : fileDocuments) {
            changeFile(d);
        }
        System.out.println("Total changed: " + count);
    }

    public void changeFile(Document fileDoc) {
        Object points = fileDoc.get("points");
        if (points instanceof Document) {
            Document pointsDoc = (Document) points;
            Set<String> keySet = pointsDoc.keySet();
            boolean changed = false;
            for (String key : keySet) {
                List<Object> values = (List<Object>) pointsDoc.get(key);
                List<String> formattedValues = new ArrayList<String>();
                for (Object f : values) {
                    if (f instanceof Double) {
                        formattedValues.add(formatter.format(f));
                        changed = true;
                    }
                }
                if (changed) {
                    pointsDoc.replace(key, formattedValues);
                }
            }
            if (changed) {
                count++;
                fileDoc.replace("points", points);
                Document doc = new Document("_id", fileDoc.get("_id"));
                filesCol.replaceOne(doc, fileDoc);
            } else {
                System.out.println("Not having doubles");
            }
        }
    }

    public static void main(String[] args) {
        ConvertPointsToString cpts = new ConvertPointsToString();
        cpts.createConnection("localhost", 27017);

        cpts.changeUserID();
        cpts.changeFileUserID();
    }
}
