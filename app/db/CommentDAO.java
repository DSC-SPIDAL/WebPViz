package db;

import com.mongodb.client.FindIterable;
import com.mongodb.util.JSON;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommentDAO {
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void addComment(int artifactId, String user, String comment) {
        MongoConnection db = MongoConnection.getInstance();
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Comment.TIME_SERIES_ID_FIELD, artifactId);

        FindIterable<Document> iterable = db.commentCol.find(oldGroupDocument);
        Document findDocument = null;
        boolean create = false;
        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument == null) {
            findDocument = new Document(Constants.Comment.TIME_SERIES_ID_FIELD, artifactId);
            create = true;
        }

        Object commentsObj = findDocument.get(Constants.Comment.COMMENTS);
        Document comments ;
        if (commentsObj == null) {
            comments = new Document();
        } else {
            comments = (Document) commentsObj;
        }

        Document newComment = new Document();
        newComment.append(Constants.Comment.TEXT, comment);
        String dateString = format.format(new Date());
        newComment.append(Constants.Comment.DATE, dateString);
        newComment.append(Constants.Comment.USER, user);

        comments.append(Long.toString(System.currentTimeMillis()), newComment);
        if (create) {
            findDocument.append(Constants.Comment.COMMENTS, comments);
            db.commentCol.insertOne(findDocument);
        } else {
            findDocument.replace(Constants.Comment.COMMENTS, comments);
            db.commentCol.replaceOne(oldGroupDocument, findDocument);
        }
    }

    public static void deleteComment(int artifactId, String user, String id) {
        MongoConnection db = MongoConnection.getInstance();
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Comment.TIME_SERIES_ID_FIELD, artifactId);

        FindIterable<Document> iterable = db.commentCol.find(oldGroupDocument);
        Document findDocument = null;

        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument != null) {
            Document commentsDoc = (Document) findDocument.get(Constants.Comment.COMMENTS);
            if (commentsDoc != null) {
                commentsDoc.remove(id);
                findDocument.replace(Constants.Comment.COMMENTS, commentsDoc);
            }
            db.commentCol.replaceOne(oldGroupDocument, findDocument);
        }
    }

    public static void updateComment(int artifactId, String user, String id, String comment) {
        MongoConnection db = MongoConnection.getInstance();
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Comment.TIME_SERIES_ID_FIELD, artifactId);

        FindIterable<Document> iterable = db.commentCol.find(oldGroupDocument);
        Document findDocument = null;

        for (Document d : iterable) {
            findDocument = d;
            break;
        }

        if (findDocument != null) {
            Document newComment = new Document();
            newComment.append(Constants.Comment.TEXT, comment);
            String dateString = format.format(new Date());
            newComment.append(Constants.Comment.DATE, dateString);
            newComment.append(Constants.Comment.USER, user);

            Document commentsDoc = (Document) findDocument.get(Constants.Comment.COMMENTS);
            if (commentsDoc != null) {
                commentsDoc.remove(id);
                commentsDoc.append(id, newComment);
                findDocument.replace(Constants.Comment.COMMENTS, commentsDoc);
            }

            db.commentCol.replaceOne(oldGroupDocument, findDocument);
        }
    }

    public static String getComments(int artifactId, String user) {
        MongoConnection db = MongoConnection.getInstance();
        Document oldGroupDocument = new Document();
        oldGroupDocument.append(Constants.Comment.TIME_SERIES_ID_FIELD, artifactId);
        FindIterable<Document> iterable = db.commentCol.find(oldGroupDocument);

        for (Document d : iterable) {
            return JSON.serialize(d);
        }
        return null;
    }
}
