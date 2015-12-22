package db;

import com.mongodb.client.FindIterable;
import models.Group;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;


public class GroupsDAO {
    public static boolean groupExistsNonDefault(Group group) {
        MongoConnection db = MongoConnection.getInstance();
        Document groupDocument = new Document();
        groupDocument.append(Constants.Group.NAME, group.name);
        if (group.userId != null) {
            groupDocument.append(Constants.Group.USER, group.userId);
        } else {
            groupDocument.append(Constants.Group.PUBLIC, true);
        }
        FindIterable<Document> iterable = db.groupsCol.find(groupDocument);
        return iterable.iterator().hasNext();
    }

    public static boolean groupExists(Group group) {
        if (Constants.Group.DEFAULT_GROUP.equals(group.name)) {
            return true;
        }
        MongoConnection db = MongoConnection.getInstance();
        Document groupDocument = new Document();
        groupDocument.append(Constants.Group.NAME, group.name);
        if (group.userId != null) {
            groupDocument.append(Constants.Group.USER, group.userId);
        } else {
            groupDocument.append(Constants.Group.PUBLIC, true);
        }
        FindIterable<Document> iterable = db.groupsCol.find(groupDocument);
        return iterable.iterator().hasNext();
    }

    public static void createDafault(String user) {
        Group defaultGroup = new Group(user, "default", "The default group", false);
        GroupsDAO.insertGroup(defaultGroup);
    }

    public static void insertGroup(Group group) {
        MongoConnection db = MongoConnection.getInstance();
        Document groupDocument = new Document();
        groupDocument.append(Constants.Group.NAME, group.name);
        groupDocument.append(Constants.Group.DESCRIPTION, group.description);
        groupDocument.append(Constants.Group.USER, group.userId);
        groupDocument.append(Constants.Group.PUBLIC, group.pub);
        db.groupsCol.insertOne(groupDocument);
    }

    public static void updateGroup(Group oldGroup, Group newGroup) {
        MongoConnection db = MongoConnection.getInstance();
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Group.NAME, oldGroup.name);
        oldGroupDocument.append(Constants.Group.USER, oldGroup.userId);

        Document groupDocument = new Document();
        groupDocument.append(Constants.Group.NAME, newGroup.name);
        groupDocument.append(Constants.Group.DESCRIPTION, newGroup.description);
        groupDocument.append(Constants.Group.USER, newGroup.userId);
        groupDocument.append(Constants.Group.PUBLIC, newGroup.pub);

        db.groupsCol.findOneAndReplace(oldGroupDocument, groupDocument);
    }

    public static void deleteGroup(Group group) {
        MongoConnection db = MongoConnection.getInstance();
        Document groupDocument = new Document();
        groupDocument.append(Constants.Group.NAME, group.name);
        groupDocument.append(Constants.Group.USER, group.userId);
        db.groupsCol.deleteOne(groupDocument);
    }

    public static List<Group> allGroups(String uid) {
        MongoConnection db = MongoConnection.getInstance();
        FindIterable<Document> iterable;
        if (uid != null) {
            iterable = db.groupsCol.find(new Document(Constants.Group.USER, uid));
        } else {
            iterable = db.groupsCol.find(new Document(Constants.Group.PUBLIC, true));
        }
        List<Group> groups = new ArrayList<Group>();
        for (Document d : iterable) {
            Group group = new Group();
            String name = (String) d.get(Constants.Group.NAME);
            String user = (String) d.get(Constants.Group.USER);
            group.description = (String) d.get(Constants.Group.DESCRIPTION);
            group.name = name;
            group.userId = user;
            Object pub = d.get(Constants.Artifact.PUBLIC);
            if (pub != null && pub instanceof Boolean) {
                group.pub = (boolean) pub;
            }
            groups.add(group);
            System.out.println(group.name);
        }
        return groups;
    }
}
