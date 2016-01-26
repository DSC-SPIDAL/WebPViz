package db;

import com.mongodb.client.FindIterable;
import com.mongodb.util.JSON;
import org.bson.Document;

/**
 * Keep track of the experiment details of a plot
 */
public class ExperimentDAO {
    private static ExperimentDAO db = new ExperimentDAO();

    public static ExperimentDAO getInstance() {
        return db;
    }

    public static void addProperty(int artifactId, String user, String key, String value) {

    }

    public static void updateProperty(int artifactId, String user, String key, String value) {

    }

    public static void removeProperty(int artifactId, String user, String key) {

    }

    public static void addDescription(int artifactId, String user, String description) {

    }

    public static void updateDescription(int artifactId, String user, String description) {

    }

    public static void addComment(int artifactId, String user, String comment) {

    }

    public static void deleteComment(int artifactId, String user, int id) {

    }

    public static void deleteComment(int artifactId, String user) {

    }

    public static String getExperiment(int artifactId, String user) {
        return null;
    }

    public static void updateExperiment(int artifactId, String user, String experimentJson) {
        MongoConnection db = MongoConnection.getInstance();
        Object data = JSON.parse(experimentJson);
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Artifact.ID_FIELD, artifactId).append(Constants.Artifact.USER, user);

        FindIterable<Document> iterable = db.experimentCol.find(oldGroupDocument);
        Document findDocument = null;
        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument != null) {
            db.experimentCol.replaceOne(oldGroupDocument, data);
        }
    }
}
