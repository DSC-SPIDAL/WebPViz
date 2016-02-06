package controllers;

import db.ArtifactDAO;
import db.GroupsDAO;
import db.TagsDAO;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.groups;

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
}
