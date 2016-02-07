package db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.FindIterable;
import models.Tag;
import org.bson.Document;

public class SearchDAO {

    public static String getArtifactsByTag(String user,String tagname){
        MongoConnection db = MongoConnection.getInstance();
        Document document = new Document();
        document.append(Constants.Tags.TAGS_FIELD + "." + tagname + "." + Constants.Tags.NAME,tagname);
        FindIterable<Document> iterable = db.plotTagsCol.find(document);

        JsonArray tidarray = new JsonArray();
        int tagcount = 1;
        for (Document d : iterable) {
            String category = (String) d.get(Constants.Tags.CATEGORY);
            JsonObject temp = new JsonObject();
            temp.addProperty("category",category);
            tidarray.add(temp);
            tagcount++;
        }
        return tidarray.toString();
    }

}
