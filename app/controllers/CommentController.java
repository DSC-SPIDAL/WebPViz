package controllers;

import db.CommentDAO;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

public class CommentController extends Controller {

    @Security.Authenticated(Secured.class)
    public static Result addComment(int artifactId, String comment) {
        User loggedInUser = User.findByEmail(request().username());
        CommentDAO.addComment(artifactId, loggedInUser.email, comment);
        return ok("{status: 'success'}").as("application/json");
    }

    @Security.Authenticated(Secured.class)
    public static Result getComments(int artifactId) {
        User loggedInUser = User.findByEmail(request().username());
        String comment = CommentDAO.getComments(artifactId, loggedInUser.email);
        if (comment != null) {
            return ok(comment).as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result removeComment(int artifactId, String id) {
        User loggedInUser = User.findByEmail(request().username());
        CommentDAO.deleteComment(artifactId, loggedInUser.email, id);
        return ok("{status: 'success'}").as("application/json");
    }

    @Security.Authenticated(Secured.class)
    public static Result updateComment(int artifactId, String id, String comment) {
        User loggedInUser = User.findByEmail(request().username());
        CommentDAO.updateComment(artifactId, loggedInUser.email, id, comment);
        return ok("{status: 'success'}").as("application/json");
    }

}
