package db;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.mongodb.client.FindIterable;
import com.mongodb.util.JSON;
import models.Tag;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TagsDAO {

    public static boolean tagExistsNonDefault(Tag tag) {
        MongoConnection db = MongoConnection.getInstance();
        Document tagDocument = new Document();
        tagDocument.append(Constants.Tags.NAME, tag.name);
        if (tag.userId != null) {
            tagDocument.append(Constants.Tags.USER, tag.userId);
        } else {
            tagDocument.append(Constants.Tags.PUBLIC, true);
        }
        FindIterable<Document> iterable = db.tagsCol.find(tagDocument);
        return iterable.iterator().hasNext();
    }

    public static boolean tagExists(Tag tag) {
        if (Constants.Tags.DEFAULT_RELEASED.equals(tag.name)||Constants.Tags.DEFAULT_INDEVELOPMENT.equals(tag.name)) {
            return true;
        }
        MongoConnection db = MongoConnection.getInstance();
        Document tagDocument = new Document();
        tagDocument.append(Constants.Tags.NAME, tag.name);
        if (tag.userId != null) {
            tagDocument.append(Constants.Tags.USER, tag.userId);
        } else {
            tagDocument.append(Constants.Tags.PUBLIC, true);
        }
        FindIterable<Document> iterable = db.tagsCol.find(tagDocument);
        return iterable.iterator().hasNext();
    }

    public static void createDafaultTags(String user) {
        Tag released = new Tag(user, "released", "release tag","lifecycle", true);
        Tag indevelopment = new Tag(user, "in-development", "development tag","lifecycle", true);
        TagsDAO.createTag(released);
        TagsDAO.createTag(indevelopment);
    }

    public static void createTag(Tag tag){
        MongoConnection db = MongoConnection.getInstance();
        Document tagsDocument = new Document();
        tagsDocument.append(Constants.Tags.NAME, tag.name);
        tagsDocument.append(Constants.Tags.DESCRIPTION, tag.description);
        tagsDocument.append(Constants.Tags.USER, tag.userId);
        tagsDocument.append(Constants.Tags.CATEGORY, tag.category);
        tagsDocument.append(Constants.Tags.PUBLIC, tag.pub);
        db.tagsCol.insertOne(tagsDocument);
    }

    public static String allTags(String uid) {
        MongoConnection db = MongoConnection.getInstance();
        FindIterable<Document> iterable;
        if (uid != null) {
            iterable = db.tagsCol.find(new Document(Constants.Tags.USER, uid));
        } else {
            iterable = db.tagsCol.find(new Document(Constants.Tags.PUBLIC, true));
        }
        List<Tag> tags = new ArrayList<Tag>();
        JsonArray tagsarray = new JsonArray();
        HashMap<String,String> distinctmap = new HashMap<String,String>();
        int tagcount = 1;
        for (Document d : iterable) {
            Tag tag = new Tag();
            String name = (String) d.get(Constants.Tags.NAME);
            String user = (String) d.get(Constants.Tags.USER);
            if(!distinctmap.containsKey(name)) {
                distinctmap.put(name, user);
                String category = (String) d.get(Constants.Tags.CATEGORY);
                JsonObject temp = new JsonObject();
                temp.addProperty("value", tagcount);
                temp.addProperty("text", name);
                temp.addProperty("category", category);
                tagsarray.add(temp);
                tagcount++;
            }
        }
        return tagsarray.toString();
    }

    public static void addTag(int artifactId, String tag, String category){
        MongoConnection db = MongoConnection.getInstance();
        Document document = new Document();
        document.append(Constants.Tags.TIME_SERIES_ID_FIELD, artifactId);

        FindIterable<Document> iterable = db.plotTagsCol.find(document);
        Document findDocument = null;
        boolean create = false;
        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument == null) {
            findDocument = new Document(Constants.Tags.TIME_SERIES_ID_FIELD, artifactId);
            create = true;
        }

        Object tagsObj = findDocument.get(Constants.Tags.TAGS_FIELD);
        Document tagsdoc ;
        if (tagsObj == null) {
            tagsdoc = new Document();
        } else {
            tagsdoc = (Document) tagsObj;
            if(tagsdoc.get(tag) != null) return;
        }


        Document newtag = new Document();
        newtag.append(Constants.Tags.NAME,tag);
        newtag.append(Constants.Tags.CATEGORY,category);
        tagsdoc.append(tag,newtag);

        if (create) {
            findDocument.append(Constants.Tags.TAGS_FIELD, tagsdoc);
            db.plotTagsCol.insertOne(findDocument);
        } else {
            findDocument.replace(Constants.Tags.TAGS_FIELD, tagsdoc);
            db.plotTagsCol.replaceOne(document, findDocument);
        }
    }

    public static void removeTag(int artifactId, String tagname){
        MongoConnection db = MongoConnection.getInstance();
        Document document = new Document();
        document.append(Constants.Tags.TIME_SERIES_ID_FIELD, artifactId);

        FindIterable<Document> iterable = db.plotTagsCol.find(document);
        Document findDocument = null;
        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument == null) {
            return;
        }

        Object tagsObj = findDocument.get(Constants.Tags.TAGS_FIELD);
        Document tagsDoc;
        if (tagsObj == null) {
            return;
        } else {
            tagsDoc = (Document) tagsObj;
            tagsDoc.remove(tagname);
            findDocument.replace(Constants.Tags.TAGS_FIELD, tagsDoc);
            db.plotTagsCol.replaceOne(document, findDocument);
            return;
        }



    }


    public static String getTags(int artifactId){
        MongoConnection db = MongoConnection.getInstance();
        Document document = new Document();
        document.append(Constants.Tags.TIME_SERIES_ID_FIELD, artifactId);

        FindIterable<Document> iterable = db.plotTagsCol.find(document);
        for (Document d : iterable) {
            return JSON.serialize(d);
        }
        return JSON.serialize("");
    }
}
