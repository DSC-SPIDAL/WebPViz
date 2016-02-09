package db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.FindIterable;
import models.Tag;
import models.TimeSeries;
import org.bson.Document;
import org.h2.command.dml.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SearchDAO {
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static List<TimeSeries> getArtifactsByTag(String user, String tagname) {
        MongoConnection db = MongoConnection.getInstance();
        Document document = new Document();
        document.append(Constants.Tags.TAGS_FIELD + "." + tagname + "." + Constants.Tags.NAME, tagname);
        FindIterable<Document> iterable = db.plotTagsCol.find(document);

        ArrayList<Integer> tidarray = new ArrayList<Integer>();
        for (Document d : iterable) {
            Integer tid = (Integer) d.get(Constants.Tags.TIME_SERIES_ID_FIELD);
            tidarray.add(tid);
        }

        QueryBuilder query = new QueryBuilder();
        if (query == null) {
            query = QueryBuilder.start();
        }
        BasicDBObject idfield = new BasicDBObject(Constants.Artifact.ID_FIELD, new BasicDBObject("$in", tidarray));
        BasicDBObject userfield;
        if (user != null) {
            userfield = new BasicDBObject(Constants.Artifact.USER, user);
        } else {
            userfield = new BasicDBObject(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE);
        }
        BasicDBObject queryobj = (BasicDBObject) query.and(idfield, userfield).get();

        FindIterable<Document> iterableTimesSeries = db.artifactCol.find(queryobj);
        return getTimeSeriesList(iterableTimesSeries);
    }

    public static List<TimeSeries> getArtifactsByTags(String user, String[] tags) {
        MongoConnection db = MongoConnection.getInstance();
        ArrayList<Integer> tidarray = new ArrayList<Integer>();
        for (String tagname : tags) {

            Document document = new Document();
            document.append(Constants.Tags.TAGS_FIELD + "." + tagname + "." + Constants.Tags.NAME, tagname);

            FindIterable<Document> iterable = db.plotTagsCol.find(document);

            for (Document d : iterable) {
                Integer tid = (Integer) d.get(Constants.Tags.TIME_SERIES_ID_FIELD);
                tidarray.add(tid);
            }
        }

        QueryBuilder query = new QueryBuilder();
        if (query == null) {
            query = QueryBuilder.start();
        }
        BasicDBObject idfield = new BasicDBObject(Constants.Artifact.ID_FIELD, new BasicDBObject("$in", tidarray));
        BasicDBObject userfield;
        if (user != null) {
            userfield = new BasicDBObject(Constants.Artifact.USER, user);
        } else {
            userfield = new BasicDBObject(Constants.Artifact.PUBLIC, Constants.Artifact.PUBLIC_TRUE);
        }
        BasicDBObject queryobj = (BasicDBObject) query.and(idfield, userfield).get();

        FindIterable<Document> iterableTimesSeries = db.artifactCol.find(queryobj);
        return getTimeSeriesList(iterableTimesSeries);
    }

    public static List<TimeSeries> getTimeSeriesList(FindIterable<Document> iterable) {
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
                if (((List) resultSetsObject).size() > 1) {
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

}
