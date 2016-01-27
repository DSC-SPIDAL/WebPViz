package db;

import com.mongodb.client.FindIterable;
import com.mongodb.util.JSON;
import org.bson.Document;

/**
 * Keep track of the experiment details of a plot
 */
public class ExperimentDAO {
    public static void addExperiment(int artifactId, String user, String experimentJson) {
        MongoConnection db = MongoConnection.getInstance();
        Object data = JSON.parse(experimentJson);
        Document findDocument;
        findDocument = new Document(Constants.Experiment.TIME_SERIES_ID_FIELD, artifactId);
        findDocument.append(Constants.Experiment.USER, user);
        findDocument.append(Constants.Experiment.EXP, data);
        db.experimentCol.insertOne(findDocument);
    }

    public static String getExperiment(int artifactId, String user) {
        MongoConnection db = MongoConnection.getInstance();
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Experiment.TIME_SERIES_ID_FIELD, artifactId).append(Constants.Experiment.USER, user);

        FindIterable<Document> iterable = db.experimentCol.find(oldGroupDocument);
        for (Document d : iterable) {
            Object exp = d.get(Constants.Experiment.EXP);
            if (exp != null) {
                return JSON.serialize(exp);
            } else {
                return null;
            }
        }
        return null;
    }

    public static void updateExperiment(int artifactId, String user, String experimentJson) {
        MongoConnection db = MongoConnection.getInstance();
        Object data = JSON.parse(experimentJson);
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Experiment.TIME_SERIES_ID_FIELD, artifactId).append(Constants.Experiment.USER, user);

        FindIterable<Document> iterable = db.experimentCol.find(oldGroupDocument);
        Document findDocument = null;

        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument != null) {
            if (findDocument.containsKey(Constants.Experiment.EXP)) {
                findDocument.remove(Constants.Experiment.EXP);
                findDocument.append(Constants.Experiment.EXP, data);
            } else {
                findDocument.append(Constants.Experiment.EXP, data);
            }
            db.experimentCol.replaceOne(oldGroupDocument, findDocument);
        } else {
            findDocument = new Document(Constants.Experiment.TIME_SERIES_ID_FIELD, artifactId);
            findDocument.append(Constants.Experiment.USER, user);
            findDocument.append(Constants.Experiment.EXP, data);
            db.experimentCol.insertOne(findDocument);
        }
    }
}
