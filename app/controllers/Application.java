package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import db.Constants;
import db.ArtifactDAO;
import db.GroupsDAO;
import models.*;
import models.utils.AppException;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipInputStream;

import static play.data.Form.form;

public class Application extends Controller {

    public static Result GO_DASHBOARD = redirect(
            controllers.routes.Application.dashboard()
    );

    public static Result index() {
        User loggedInUser = User.findByEmail(session().get("email"));
        return ok(index.render(loggedInUser));
    }

    public static Result logout() {
        session().clear();
        return redirect(routes.Application.index());
    }

    public static Result login() {
        return ok(login.render(form(Login.class)));
    }

    @Security.Authenticated(Secured.class)
    public static Result dashboard() {
        ArtifactDAO db = ArtifactDAO.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
    }

    @Security.Authenticated(Secured.class)
    public static Result groupDashboard(String group) {
        ArtifactDAO db = ArtifactDAO.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        Group g = new Group(loggedInUser.id, group);
        if (GroupsDAO.groupExists(g)) {
            return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(g), GroupsDAO.allGroups(), true, group));
        } else {
            return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result about() {
        User loggedInUser = User.findByEmail(request().username());
        return ok(about.render(loggedInUser));
    }

    @Security.Authenticated(Secured.class)
    public static Result delete(int timeSeriesId){
        //TODO delete time series
        ArtifactDAO db = ArtifactDAO.getInstance();
        db.deleteTimeSeries(timeSeriesId);
        return GO_DASHBOARD;
    }

    @Security.Authenticated(Secured.class)
    public static Result upload() throws IOException {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart resultSet = body.getFile("file");
        String originalFileName = resultSet.getFilename();
        String name = resultSet.getFile().getName();
        String[] desc = body.asFormUrlEncoded().get("desc");
        String[] grp = body.asFormUrlEncoded().get("group");
        String description = "No description";
        String group = Constants.Group.DEFAULT_GROUP;

        if (desc.length >= 1) {
            description = desc[0];
        }

        if (grp.length >= 1) {
            group = grp[0];
        }

        File file = resultSet.getFile();
        Logger.info(String.format("User %s uploaded a new result of name %s", loggedInUser.id, originalFileName));
        boolean isZipped = new ZipInputStream(new FileInputStream(file)).getNextEntry() != null;
        try {
            if (isZipped) {
                db.insertZipFile(originalFileName, description, loggedInUser.id, file, group);
            } else {
                db.insertSingleFile(originalFileName, description, loggedInUser.id, file, group);
            }
        } catch (Exception e) {
            Logger.error("Failed to create time series from zip", e);
            return badRequest(dashboard.render(loggedInUser, true, "Failed to read zip file.", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
        return GO_DASHBOARD;
    }

    @Security.Authenticated(Secured.class)
    public static Result updateFile() {
        System.out.println("Update");
        DynamicForm form = Form.form().bindFromRequest();

        String description, group, id;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(dashboard.render(loggedInUser, true, "Update parameters should be present", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
        id = form.get("id");
        description = form.get("desc");
        group = form.get("group");


        TimeSeries oldGroup = new TimeSeries();
        oldGroup.id = Integer.parseInt(id);
        oldGroup.uploaderId = loggedInUser.id;

        TimeSeries newTimeSeries = new TimeSeries();
        newTimeSeries.description = description;
        newTimeSeries.group = group;

        if (db.timeSeriesExists(oldGroup)) {
            System.out.println("Exists");
            db.updateTimeSeries(oldGroup, newTimeSeries);
            return GO_DASHBOARD;
        } else {
            //
            System.out.println("non exisits");
            return badRequest(dashboard.render(loggedInUser, true, "Update non-existing file", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result visualize(int resultSetId, int timeSeriesId) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        ResultSet r = db.individualFile(timeSeriesId, resultSetId);
        if (r != null) {
            return ok(resultset.render(loggedInUser, resultSetId, timeSeriesId, r.name));
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Plot cannot be found.", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result visualizeSingle(int timeSeriesId) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        ResultSet r = db.individualFile(timeSeriesId);
        if (r != null) {
            return ok(resultset.render(loggedInUser, r.id, timeSeriesId, r.name));
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Plot cannot be found.", db.timeSeriesList(), GroupsDAO.allGroups(), false, null));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result visualizeTimeSeries(int timeSeriesId) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        TimeSeries timeSeriesProps = db.timeSeries(timeSeriesId);
        int id = timeSeriesProps.id;
        String name = timeSeriesProps.name;
        return ok(timeseries.render(loggedInUser, id, name));
    }

    public static Result uploadGet() {
        return redirect(controllers.routes.Application.dashboard());
    }

    public static Result authenticateGet() {
        return redirect(controllers.routes.Application.login());
    }

    public static Result addUser() {
        Form<SignUp> loginForm = form(SignUp.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest();
        } else {
            session("email", loginForm.get().email);
            return GO_DASHBOARD;
        }
    }

    public static Result resultssetall(int tid, int rid) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        String r = db.queryFile(tid, rid);
        JsonNode result = Json.parse(r);
        return ok(result);
    }

    public static Result timeseries(int id) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        String r = db.getArtifact(id);
        JsonNode result = Json.parse(r);
        return ok(result);
    }

    public static Result resultset(Integer rid, Integer tid) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        ResultSet r = db.individualFile(tid, rid);
        ObjectNode result = Json.newObject();
        result.put("id", rid);
        result.put("name", r.name);
        result.put("desc", r.description);
        result.put("uploaded", User.findById(r.uploaderId).email);
        result.put("fileName", r.fileName);

        List<Cluster> clusters = db.clusters(tid, rid);
        result.put("clusters", Json.toJson(clusters));

        return ok(result);
    }

    /**
     * Handle login form submission.
     *
     * @return Dashboard if auth OK or login form if auth KO
     */
    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();

        if (loginForm.hasErrors()) {
            return badRequest(login.render(loginForm));
        } else {
            session("email", loginForm.get().email);
            return GO_DASHBOARD;
        }
    }


    /**
     * Login class used by Login Form.
     */
    public static class Login {

        @Constraints.Required
        public String email;

        @Constraints.Required
        public String password;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {

            User user = null;
            try {
                user = User.authenticate(email, password);
            } catch (AppException e) {
                Logger.error("Something went wrong during authentication.", e);
                return "Technical error, please retry later.";
            }

            if (user == null) {
                String errMessage = "Invalid user or password.";
                Logger.warn(errMessage);
                return errMessage;
            }

            return null;
        }

    }

    public static class SignUp {

        @Constraints.Required
        public String email;

        @Constraints.Required
        public String password;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {

            User user = null;
            try {
                user = User.create(email, password, "Wickramasinghe");
            } catch (AppException e) {
                Logger.error("Something went wrong during authentication.", e);
                return "Technical error, please retry later.";
            }

            if (user == null) {
                String errMessage = "Invalid user or password.";
                Logger.warn(errMessage);
                return errMessage;
            }

            return null;
        }

    }
}
