package controllers;

import db.ArtifactDAO;
import db.GroupsDAO;
import models.Group;
import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.groups;

public class GroupController extends Controller {
    public static Result GO_GROUPS = redirect(
            controllers.routes.GroupController.groups()
    );

    @Security.Authenticated(Secured.class)
    public static Result groups() {
        ArtifactDAO db = ArtifactDAO.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        return ok(groups.render(loggedInUser, false, null, GroupsDAO.allGroups()));
    }

    @Security.Authenticated(Secured.class)
    public static Result newGroup() {
        DynamicForm form = Form.form().bindFromRequest();

        String name, description;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(groups.render(loggedInUser, true, "Should give group info", GroupsDAO.allGroups()));
        }
        name = form.get("name");
        description = form.get("desc");
        Group group = new Group(loggedInUser.id, name);
        if (!GroupsDAO.groupExists(group)) {
            GroupsDAO.insertGroup(new Group(loggedInUser.id, name, description));
            return GO_GROUPS;
        } else {
            //
            return badRequest(groups.render(loggedInUser, true, "Existing group", GroupsDAO.allGroups()));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result updateGroup() {
        DynamicForm form = Form.form().bindFromRequest();

        String name, description;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(groups.render(loggedInUser, true, "Should give group info", GroupsDAO.allGroups()));
        }
        name = form.get("name");
        description = form.get("desc");
        System.out.println(name);
        Group oldGroup = new Group(loggedInUser.id, name);
        if (GroupsDAO.groupExists(oldGroup)) {
            GroupsDAO.updateGroup(oldGroup, new Group(loggedInUser.id, name, description));
            return GO_GROUPS;
        } else {
            //
            return badRequest(groups.render(loggedInUser, true, "Non existing group", GroupsDAO.allGroups()));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result removeGroup(String name) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        Group group = new Group(loggedInUser.id, name);
        if (GroupsDAO.groupExists(group)) {
            GroupsDAO.deleteGroup(group);
            return GO_GROUPS;
        } else {
            //
            return badRequest(groups.render(loggedInUser, true, "Non existing group", GroupsDAO.allGroups()));
        }
    }
}
