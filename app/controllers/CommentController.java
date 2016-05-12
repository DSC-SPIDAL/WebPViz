package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import db.CommentDAO;
import db.Constants;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

public class CommentController extends Controller {

    @Security.Authenticated(Secured.class)
    public Result addComment() {
        User loggedInUser = User.findByEmail(request().username());

        JsonNode json = request().body().asJson();
        String text = json.get(Constants.Comment.TEXT).asText();
        int artifactId = json.get(Constants.Comment.TIME_SERIES_ID_FIELD).asInt();

        CommentDAO.addComment(artifactId, loggedInUser.email, text);
        return ok("{status: 'success'}").as("application/json");
    }

    @Security.Authenticated(Secured.class)
    public Result getComments(int artifactId) {
        User loggedInUser = User.findByEmail(request().username());
        String comment = CommentDAO.getComments(artifactId, loggedInUser.email);
        if (comment != null) {
            return ok(comment).as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }

    @Security.Authenticated(Secured.class)
    public Result removeComment(int artifactId, String id) {
        User loggedInUser = User.findByEmail(request().username());
        CommentDAO.deleteComment(artifactId, loggedInUser.email, id);
        return ok("{status: 'success'}").as("application/json");
    }

    @Security.Authenticated(Secured.class)
    public Result updateComment(int artifactId, String id, String comment) {
        User loggedInUser = User.findByEmail(request().username());
        CommentDAO.updateComment(artifactId, loggedInUser.email, id, comment);
        return ok("{status: 'success'}").as("application/json");
    }

}
