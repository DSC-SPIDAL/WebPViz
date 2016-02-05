package db;


import com.mongodb.client.FindIterable;
import models.Group;
import models.Tag;
import org.bson.Document;

import java.util.ArrayList;
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
        Tag released = new Tag("webplotviziu", "released", "release tag","lifecycle", true);
        Tag indevelopment = new Tag("webplotviziu", "in-development", "development tag","lifecycle", true);
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

    public static List<Tag> allTags(String uid) {
        MongoConnection db = MongoConnection.getInstance();
        FindIterable<Document> iterable;
        if (uid != null) {
            iterable = db.tagsCol.find(new Document(Constants.Tags.USER, uid));
        } else {
            iterable = db.tagsCol.find(new Document(Constants.Tags.PUBLIC, true));
        }
        List<Tag> tags = new ArrayList<Tag>();
        for (Document d : iterable) {
            Tag tag = new Tag();
            String name = (String) d.get(Constants.Tags.NAME);
            String user = (String) d.get(Constants.Tags.USER);
            tag.description = (String) d.get(Constants.Tags.DESCRIPTION);
            tag.category = (String) d.get(Constants.Tags.CATEGORY);
            tag.name = name;
            tag.userId = user;
            Object pub = d.get(Constants.Artifact.PUBLIC);
            if (pub != null && pub instanceof Boolean) {
                tag.pub = (boolean) pub;
            }
            tags.add(tag);
            System.out.println(tag.name);
        }
        return tags;
    }





}
