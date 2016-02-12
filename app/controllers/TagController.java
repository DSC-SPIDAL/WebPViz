package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import db.ArtifactDAO;
import db.Constants;
import db.TagsDAO;
import models.Tag;
import models.User;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

public class TagController extends Controller {

    public static Result getAllTags() {
        User loggedInUser = User.findByEmail(request().username());
        String tags = null;
        if(loggedInUser == null){
             tags = TagsDAO.allTags(null);
        }else {
             tags = TagsDAO.allTags(loggedInUser.email);
        }
        if (tags != null) {
            return ok(tags).as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result createTag() {

        String tagname, description, category, fromGroup;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        JsonNode json = request().body().asJson();
        tagname = json.get(Constants.Tags.NAME).asText();
        description = json.get(Constants.Tags.DESCRIPTION).asText();
        category = json.get(Constants.Tags.CATEGORY).asText();
        Tag tag = new Tag(loggedInUser.email,tagname,description,category,false);
        if(!TagsDAO.tagExists(tag)){
            TagsDAO.createTag(tag);
            return ok("{status: 'success'}").as("application/json");
        }else{
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result addTag(int artifactId) {
        JsonNode json = request().body().asJson();
        String tagname = json.get(Constants.Tags.NAME).asText();
        String category = json.get(Constants.Tags.CATEGORY).asText();
        TagsDAO.addTag(artifactId,tagname,category);
        return ok("{status: 'success'}").as("application/json");
    }

    @Security.Authenticated(Secured.class)
    public static Result removeTag(int artifactId) {
        JsonNode json = request().body().asJson();
        String tagname = json.get(Constants.Tags.NAME).asText();
        TagsDAO.removeTag(artifactId,tagname);
        return ok("{status: 'success'}").as("application/json");
    }

    public static Result getTags(int artifactId) {
        String tags = TagsDAO.getTags(artifactId);
        if (tags != null) {
            return ok(tags).as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }
}
