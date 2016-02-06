package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import db.ArtifactDAO;
import db.Constants;
import db.GroupsDAO;
import db.TagsDAO;
import models.Group;
import models.Tag;
import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.dashboard;
import views.html.groups;
import views.html.info;

public class TagController extends Controller {

    @Security.Authenticated(Secured.class)
    public static Result getTags() {
        User loggedInUser = User.findByEmail(request().username());
        String tags = TagsDAO.allTags(loggedInUser.email);
        if (tags != null) {
            return ok(tags).as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result createTag() {
        DynamicForm form = Form.form().bindFromRequest();

        String tagname, description, category, fromGroup;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest("{status: 'fail'}").as("application/json");
        }
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
}
