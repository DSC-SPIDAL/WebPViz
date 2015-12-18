    package edu.iu.soic.dsc;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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

        cpts.changeFileCollection();
    }
}
