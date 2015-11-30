package controllers;

import db.MongoDB;
import models.Group;
import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.dashboard;
import views.html.groups;

public class GroupController extends Controller {
    public static Result GO_GROUPS = redirect(
            controllers.routes.GroupController.groups()
    );

    @Security.Authenticated(Secured.class)
    public static Result groups() {
        MongoDB db = MongoDB.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        return ok(groups.render(loggedInUser, false, null, db.allGroups()));
    }

    @Security.Authenticated(Secured.class)
    public static Result newGroup() {
        DynamicForm form = Form.form().bindFromRequest();

        String name, description;
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(groups.render(loggedInUser, true, "Should give group info", db.allGroups()));
        }
        name = form.get("name");
        description = form.get("desc");
        Group group = new Group(loggedInUser.id, name);
        if (!db.groupExists(group)) {
            db.insertGroup(new Group(loggedInUser.id, name, description));
            return GO_GROUPS;
        } else {
            //
            return badRequest(groups.render(loggedInUser, true, "Existing group", db.allGroups()));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result updateGroup() {
        DynamicForm form = Form.form().bindFromRequest();

        String name, description;
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(groups.render(loggedInUser, true, "Should give group info", db.allGroups()));
        }
        name = form.get("name");
        description = form.get("desc");
        System.out.println(name);
        Group oldGroup = new Group(loggedInUser.id, name);
        if (db.groupExists(oldGroup)) {
            db.updateGroup(oldGroup, new Group(loggedInUser.id, name, description));
            return GO_GROUPS;
        } else {
            //
            return badRequest(groups.render(loggedInUser, true, "Non existing group", db.allGroups()));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result removeGroup(String name) {
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        Group group = new Group(loggedInUser.id, name);
        if (db.groupExists(group)) {
            db.deleteGroup(group);
            return GO_GROUPS;
        } else {
            //
            return badRequest(groups.render(loggedInUser, true, "Non existing group", db.allGroups()));
        }
    }
}
