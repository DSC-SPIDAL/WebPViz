package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import db.Constants;
import db.ArtifactDAO;
import db.GroupsDAO;
import models.*;
import models.utils.AppException;
import models.utils.Hash;
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
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        String from = request().getQueryString("from");
        if (from != null) {
            Map<String,String> anyData = new HashMap();
            anyData.put("targetUrl", from);
            Form<Login> loginForm = form(Login.class).bind(anyData);
            return ok(login.render(loginForm, from));
        } else {
            return ok(login.render(form(Login.class), ""));
        }
    }

    public static Result reg() {
        return ok(register.render(form(SignUp.class), ""));
    }

    public static Result register() {
        Form<SignUp> loginForm = form(SignUp.class).bindFromRequest();

        if (loginForm.hasErrors()) {
            return badRequest(register.render(loginForm, ""));
        } else {
            try {
                User user = User.create(loginForm.get().email, loginForm.get().password);
                GroupsDAO.createDafault(user.id);
            } catch (AppException e) {
                e.printStackTrace();
            }
            session("email", loginForm.get().email);
            return GO_DASHBOARD;
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result dashboard() {
        ArtifactDAO db = ArtifactDAO.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), false, null, false, "Dashboard"));
    }


    public static Result publicDashboard() {
        ArtifactDAO db = ArtifactDAO.getInstance();
        String email = session().get("email");
        User loggedInUser = null;
        if (email != null) {
            loggedInUser = User.findByEmail(email);
        }
        return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(-1), GroupsDAO.allGroups(-1), false, null, true, "Public"));
    }

    @Security.Authenticated(Secured.class)
    public static Result groupDashboard(String group) {
        ArtifactDAO db = ArtifactDAO.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        Group g = new Group(loggedInUser.id, group);
        if (GroupsDAO.groupExists(g)) {
            return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(g, loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), true, group, false, "Dashboard"));
        } else {
            return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), false, null, false, "Dashboard"));
        }
    }

    public static Result groupDashboardPublic(String group) {
        ArtifactDAO db = ArtifactDAO.getInstance();

        Group g = new Group(-1, group);
        String email = session().get("email");
        User loggedInUser = null;
        if (email != null) {
            loggedInUser = User.findByEmail(email);
        }
        if (GroupsDAO.groupExists(g)) {
            return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(g, -1), GroupsDAO.allGroups(-1), true, group, true, "Public"));
        } else {
            return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(-1), GroupsDAO.allGroups(-1), false, null, true, "Public"));
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
        User loggedInUser = User.findByEmail(request().username());
        ArtifactDAO db = ArtifactDAO.getInstance();
        db.deleteTimeSeries(timeSeriesId, loggedInUser.id);
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
        String[] fromGroupForm = body.asFormUrlEncoded().get("from_group");

        String description = "No description";
        String group = Constants.Group.DEFAULT_GROUP;
        String fromGroup = null;

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
            return badRequest(dashboard.render(loggedInUser, true, "Failed to read zip file.", db.timeSeriesList(loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), false, null, false, "Dashboard"));
        }
        if (fromGroupForm == null) {
            return GO_DASHBOARD;
        } else {
            return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(new Group(loggedInUser.id, group), loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), true, group, false, "Dashboard"));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result updateFile() {
        System.out.println("Update");
        DynamicForm form = Form.form().bindFromRequest();

        String description, group, id, fromGroup = null, pub;
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        if (form.data().size() == 0) {
            return badRequest(dashboard.render(loggedInUser, true, "Update parameters should be present", db.timeSeriesList(loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), false, null, false, "Dashboard"));
        }
        id = form.get("id");
        description = form.get("desc");
        group = form.get("group");
        fromGroup = form.get("from_group");
        pub = form.get("pub");
        boolean pubVal = false;
        if (pub != null && ("on".equals(pub) || "checked".equals(pub))) {
            pubVal = true;
        }

        TimeSeries oldGroup = new TimeSeries();
        oldGroup.id = Integer.parseInt(id);
        oldGroup.uploaderId = loggedInUser.id;

        TimeSeries newTimeSeries = new TimeSeries();
        newTimeSeries.description = description;
        newTimeSeries.group = group;
        newTimeSeries.pub = pubVal;
        newTimeSeries.id = loggedInUser.id;

        if (db.timeSeriesExists(oldGroup)) {
            System.out.println("Exists");
            db.updateTimeSeries(oldGroup, newTimeSeries);
            if (fromGroup == null) {
                return GO_DASHBOARD;
            } else {
                return ok(dashboard.render(loggedInUser, false, null, db.timeSeriesList(new Group(loggedInUser.id, group), loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), true, group, false, "Dashboard"));
            }
        } else {
            //
            System.out.println("non exisits");
            return badRequest(dashboard.render(loggedInUser, true, "Update non-existing file", db.timeSeriesList(loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), false, null, false, "Dashboard"));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result savePlot() {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        JsonNode json = request().body().asJson();
        String body = Json.stringify(json);
        int timeSeriesId = 0;
        JsonNode camera = json.get("camera");
        TimeSeries tid = new TimeSeries();
        String timeSeries = json.get("tid").asText();
        if (timeSeries != null) {
            timeSeriesId = Integer.parseInt(timeSeries);
            tid.uploaderId = loggedInUser.id;
            tid.id = timeSeriesId;
            db.updateArtifactSetting(tid, body);
            return ok("success");
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Update non-existing file", db.timeSeriesList(loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), false, null, false, "Dashboard"));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result singlePage(int timeSeriesId) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        ResultSet r = db.individualFile(timeSeriesId, loggedInUser.id);
        if (r != null) {
            return ok(resultset.render(loggedInUser, r.id, timeSeriesId, r.name));
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Plot cannot be found.", db.timeSeriesList(loggedInUser.id), GroupsDAO.allGroups(loggedInUser.id), false, null, false, "Dashboard"));
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result timeSeriesPage(int timeSeriesId) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        TimeSeries timeSeriesProps = db.timeSeries(timeSeriesId, loggedInUser.id);
        int id = timeSeriesProps.id;
        String name = timeSeriesProps.name;
        return ok(timeseries.render(loggedInUser, id, name));
    }

    public static Result singlePublicPage(int timeSeriesId) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        ResultSet r = db.individualFile(timeSeriesId, -1);
        String email = session().get("email");
        User loggedInUser = null;
        if (email != null) {
            loggedInUser = User.findByEmail(email);
        }
        if (r != null) {
            return ok(resultset.render(loggedInUser, r.id, timeSeriesId, r.name));
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Plot cannot be found.", db.timeSeriesList(-1), GroupsDAO.allGroups(-1), false, null, true, "Public"));
        }
    }

    public static Result timeSeriesPublicPage(int timeSeriesId) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        TimeSeries timeSeriesProps = db.timeSeries(timeSeriesId, -1);
        int id = timeSeriesProps.id;
        String name = timeSeriesProps.name;
        String email = session().get("email");
        User loggedInUser = null;
        if (email != null) {
            loggedInUser = User.findByEmail(email);
        }
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

    /**
     * Get the individual plot with the clusters, points and edges
     * @param tid artifact id
     * @param rid file id
     * @return
     */
    @Security.Authenticated(Secured.class)
    public static Result getFile(int tid, int rid) {
        long t0 = System.currentTimeMillis();
        User loggedInUser = User.findByEmail(request().username());
        ArtifactDAO db = ArtifactDAO.getInstance();
        String r = db.getFile(tid, rid, loggedInUser.id);
        Logger.info("Time: " + (System.currentTimeMillis() - t0));
        if (r != null) {
            return ok(r).as("application/json");
        } else {
            return notFound("{found: false}");
        }
    }

    /**
     * Get the individual plot with the clusters, points and edges
     * @param tid artifact id
     * @param rid file id
     * @return
     */
    public static Result getPublicFile(int tid, int rid) {
        long t0 = System.currentTimeMillis();
        ArtifactDAO db = ArtifactDAO.getInstance();
        String r = db.getFile(tid, rid, -1);
        Logger.info("Time: " + (System.currentTimeMillis() - t0));
        if (r != null) {
            return ok(r).as("application/json");
        } else {
            return notFound("{found: false}");
        }
    }

    /**
     * Get the artifact information.
     * @param id artifact id
     * @return
     */
    @Security.Authenticated(Secured.class)
    public static Result getArtifact(int id) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        String r = db.getArtifact(id, loggedInUser.id);
        JsonNode result = Json.parse(r);
        return ok(result);
    }

    /**
     * Get the artifact information.
     * @param id artifact id
     * @return
     */
    public static Result getPublicArtifact(int id) {
        ArtifactDAO db = ArtifactDAO.getInstance();
        String r = db.getArtifact(id, -1);
        JsonNode result = Json.parse(r);
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
            return badRequest(login.render(loginForm, ""));
        } else {
            session("email", loginForm.get().email);
            String targetUrl = loginForm.get().targetUrl;
            if (targetUrl != null && !"".equals(targetUrl)) {
                return redirect(targetUrl);
            }
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
        public String targetUrl;
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
            User user = User.findByEmail(email);
            if (user != null) {
                String errMessage = "Invalid user or password.";
                Logger.warn(errMessage);
                return errMessage;
            }
            return null;
        }

    }
}
