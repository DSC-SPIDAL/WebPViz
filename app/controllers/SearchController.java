package controllers;


import db.SearchDAO;
import db.TagsDAO;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

public class SearchController extends Controller {
    @Security.Authenticated(Secured.class)
    public static Result searchArtifactsByTag(String tagname) {
        User loggedInUser = User.findByEmail(request().username());
        String tags = SearchDAO.getArtifactsByTag(loggedInUser.email,tagname);
        if (tags != null) {
            return ok(tags).as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }
}
