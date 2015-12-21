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
import views.html.dashboard;
import views.html.groups;

public class GroupController extends Controller {
    public static Result GO_GROUPS = redirect(
            controllers.routes.GroupController.groups()
    );

    public static Result GO_DASHBOARD = redirect(
            controllers.routes.Application.dashboard()
    );

    @Security.Authenticated(Secured.class)
    public  Result groups() {
        ArtifactDAO db = ArtifactDAO.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        return ok(groups.render(loggedInUser, false, null, GroupsDAO.allGroups()));
    }

    @Security.Authenticated(Secured.class)
    public  Result newGroup() {
        DynamicForm form = Form.form().bindFromRequest();

        String name, description, fromGroup;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(dashboard.render(loggedInUser, true, "No data", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
        name = form.get("name");
        description = form.get("desc");
        fromGroup = form.get("from_group");
        Group group = new Group(loggedInUser.id, name);
        if (!GroupsDAO.groupExists(group)) {
            GroupsDAO.insertGroup(new Group(loggedInUser.id, name, description));
            if (fromGroup == null) {
                return GO_DASHBOARD;
            } else {
                return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(new Group(loggedInUser.id, name)), GroupsDAO.allGroups(), true, name));
            }
        } else {
            //
            return badRequest(dashboard.render(loggedInUser, true, "Existing group", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
    }

    @Security.Authenticated(Secured.class)
    public  Result updateGroup() {
        DynamicForm form = Form.form().bindFromRequest();

        String name, description, fromGroup = null;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(dashboard.render(loggedInUser, true, "No group information", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
        name = form.get("name");
        description = form.get("desc");
        fromGroup = form.get("from_group");
        System.out.println(name);
        Group oldGroup = new Group(loggedInUser.id, name);
        if (GroupsDAO.groupExists(oldGroup)) {
            GroupsDAO.updateGroup(oldGroup, new Group(loggedInUser.id, name, description));
            if (fromGroup == null) {
                return GO_DASHBOARD;
            } else {
                return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(new Group(loggedInUser.id, name)), GroupsDAO.allGroups(), true, name));
            }
        } else {
            //
            return badRequest(dashboard.render(loggedInUser, true, "Non Existing group", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
    }

    @Security.Authenticated(Secured.class)
    public  Result removeGroup(String name) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        Group group = new Group(loggedInUser.id, name);
        if (GroupsDAO.groupExists(group)) {
            GroupsDAO.deleteGroup(group);
            return GO_DASHBOARD;
        } else {
            //
            return badRequest(dashboard.render(loggedInUser, true, "Non Existing group", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
    }
}
