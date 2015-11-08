package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import db.MongoDB;
import models.*;
import models.utils.AppException;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipInputStream;

import static play.data.Form.form;

public class Application extends Controller {

    public static Result GO_DASHBOARD = redirect(
            controllers.routes.Application.dashboard()
    );

    public static Result index() {
        User loggedInUser = User.findByEmail(session().get("email"));
        return ok(index.render(loggedInUser, ResultSet.all()));
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
        MongoDB db = MongoDB.getInstance();

        User loggedInUser = User.findByEmail(request().username());
        return ok(dashboard.render(loggedInUser, false, null, db.getAllResultSet(), db.getAllTimeSeries()));
    }

    @Security.Authenticated(Secured.class)
    public static Result about() {
        User loggedInUser = User.findByEmail(request().username());
        return ok(about.render(loggedInUser));
    }

    @Security.Authenticated(Secured.class)
    public static Result delete(int timeSeriesId){
        //TODO delete time series
        MongoDB db = MongoDB.getInstance();
        db.deleteTimeSeries(timeSeriesId);
        return GO_DASHBOARD;
    }
    @Security.Authenticated(Secured.class)
    public static Result upload() throws IOException {
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart resultSet = body.getFile("file");
        String originalFileName = resultSet.getFilename();
        String name = resultSet.getFile().getName();
        String[] desc = body.asFormUrlEncoded().get("desc");
        String description = "No description";

        if (ResultSet.findByName(name) != null) {
            return badRequest(dashboard.render(loggedInUser, true, "Result set with same name exists.", ResultSet.all(), TimeSeries.all()));
        }
        if (desc.length >= 1) {
            description = desc[0];
        }

        File file = resultSet.getFile();
        Logger.info(String.format("User %s uploaded a new result of name %s", loggedInUser.id, originalFileName));
        boolean isZipped = new ZipInputStream(new FileInputStream(file)).getNextEntry() != null;
        try {
            if (isZipped) {
                db.insertZipFile(originalFileName, description, loggedInUser.id, file);
            } else {
                db.insertSingleFile(originalFileName, description, loggedInUser.id, file);
            }
        } catch (Exception e) {
            Logger.error("Failed to create time series from zip", e);
            return badRequest(dashboard.render(loggedInUser, true, "Failed to read zip file.", ResultSet.all(), TimeSeries.all()));
        }
        return GO_DASHBOARD;
    }

    public static Result visualize(int resultSetId, int timeSeriesId) {
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());
        ResultSet r = db.queryResultSetProsById(timeSeriesId, resultSetId);
        if (r != null) {
            return ok(resultset.render(loggedInUser, resultSetId, timeSeriesId, r.name));
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Plot cannot be found.", ResultSet.all(), TimeSeries.all()));
        }
    }

    public static Result visualizeTimeSeries(int timeSeriesId) {
        MongoDB db = MongoDB.getInstance();
        User loggedInUser = User.findByEmail(request().username());

        TimeSeries timeSeriesProps = db.queryTimeSeriesProperties(timeSeriesId);
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
        MongoDB db = MongoDB.getInstance();
        String r = db.queryFile(tid, rid);
        JsonNode result = Json.parse(r);
        return ok(result);
    }

    public static Result timeseries(int id) {
        MongoDB db = MongoDB.getInstance();
        String r = db.queryTimeSeries(id);
        JsonNode result = Json.parse(r);
        return ok(result);
    }

    public static Result resultset(Integer rid, Integer tid) {
        MongoDB db = MongoDB.getInstance();
        ResultSet r = db.queryResultSetProsById(tid, rid);
        ObjectNode result = Json.newObject();
        result.put("id", rid);
        result.put("name", r.name);
        result.put("desc", r.description);
        result.put("uploaded", User.findById(r.uploaderId).email);
        result.put("fileName", r.fileName);

        List<Cluster> clusters = db.getClusters(tid, rid);
        result.put("clusters", Json.toJson(clusters));

        return ok(result);
    }

    public static Result cluster(Integer rid, Integer cid, Iterator tid) {
        Cluster c = Cluster.findByClusterId(rid, cid);
        ObjectNode result = Json.newObject();
        result.put("id", c.id);
        result.put("rid", c.resultSet);
        result.put("cid", c.cluster);
        result.put("points", Json.toJson(Point.findByCluster(rid, c.id)));

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
