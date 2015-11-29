package controllers;

import db.MongoDB;
import models.Group;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.dashboard;

public class GroupController extends Controller {
    public static Result GO_DASHBOARD = redirect(
            controllers.routes.Application.dashboard()
    );

    public static Result newGroup(String name, String description) {
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        Group group = new Group(name);
        if (!db.groupExists(group)) {
            db.insertGroup(new Group(name, description));
            return ok(dashboard.render(loggedInUser, false, null, db.individualFiles(), db.timeSeriesList()));
        } else {
            //
            return null;
        }
    }

    public static Result updateGroup(String name, String newName, String newDesc) {
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        Group oldGroup = new Group(name);
        if (!db.groupExists(oldGroup)) {
            db.updateGroup(oldGroup, new Group(newName, newDesc));
            return ok(dashboard.render(loggedInUser, false, null, db.individualFiles(), db.timeSeriesList()));
        } else {
            //
            return null;
        }
    }

    public static Result removeGroup(String name) {
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        Group group = new Group(name);
        if (!db.groupExists(group)) {
            db.deleteGroup(group);
            return ok(dashboard.render(loggedInUser, false, null, db.individualFiles(), db.timeSeriesList()));
        } else {
            //
            return null;
        }
    }
}
