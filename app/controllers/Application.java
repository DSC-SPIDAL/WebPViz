package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import models.utils.AppException;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        return ok(index.render(null, ResultSet.all()));
    }

    public static Result login() {
        return ok(login.render(form(Login.class)));
    }

    @Security.Authenticated(Secured.class)
    public static Result dashboard() {
        User loggedInUser = User.findByEmail(request().username());
        return ok(dashboard.render(loggedInUser, false, null, ResultSet.all(), TimeSeries.all()));
    }

    @Security.Authenticated(Secured.class)
    public static Result upload() throws IOException {
        User loggedInUser = User.findByEmail(request().username());
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart resultSet = body.getFile("file");
        String[] name = body.asFormUrlEncoded().get("name");
        String[] desc = body.asFormUrlEncoded().get("desc");
        String description = "No description";
        if (name.length < 1 || name[0].isEmpty() || name[0].equalsIgnoreCase(" ")) {
            return badRequest(dashboard.render(loggedInUser, true, "Empty or blank name.", ResultSet.all(), TimeSeries.all()));
        }

        if (ResultSet.findByName(name[0]) != null) {
            return badRequest(dashboard.render(loggedInUser, true, "Result set with same name exists.", ResultSet.all(), TimeSeries.all()));
        }

        if (desc.length >= 1) {
            description = desc[0];
        }

        if (resultSet != null) {
            File file = resultSet.getFile();
            Logger.info(String.format("User %s uploaded a new result of name %s", loggedInUser.id, name[0]));
            ResultSet.createFromFile(name[0], description, loggedInUser, file);
            return GO_DASHBOARD;
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Missing file.", ResultSet.all(), TimeSeries.all()));
        }
    }


    @Security.Authenticated(Secured.class)
    public static Result uploadFiles() throws IOException {
        User loggedInUser = User.findByEmail(request().username());
        Http.MultipartFormData body = request().body().asMultipartFormData();
        List<Http.MultipartFormData.FilePart> resultSet = body.getFiles();
        String[] name = body.asFormUrlEncoded().get("name");
        String[] desc = body.asFormUrlEncoded().get("desc");
        String description = "No description";
        if (name.length < 1 || name[0].isEmpty() || name[0].equalsIgnoreCase(" ")) {
            return badRequest(dashboard.render(loggedInUser, true, "Empty or blank name.", ResultSet.all(), TimeSeries.all()));
        }

        if (ResultSet.findByName(name[0]) != null) {
            return badRequest(dashboard.render(loggedInUser, true, "Result set with same name exists.", ResultSet.all(), TimeSeries.all()));
        }

        if (desc.length >= 1) {
            description = desc[0];
        }

        if (resultSet != null) {

            Logger.info(String.format("User %s uploaded a new set of time series files of name %s", loggedInUser.id, name[0]));
            TimeSeries.createFromFiles(name[0], description, loggedInUser, resultSet);

//            File file = resultSet.getFile();
//            Logger.info(String.format("User %s uploaded a new result of name %s", loggedInUser.id, name[0]));
//            ResultSet.createFromFile(name[0], description, loggedInUser, file);
            return GO_DASHBOARD;
        } else {
            return badRequest(dashboard.render(loggedInUser, true, "Missing file.", ResultSet.all(), TimeSeries.all()));
        }


    }

    public static Result visualize(Long resultSetId) {
        User loggedInUser = User.findByEmail(request().username());
        ResultSet r = ResultSet.findById(resultSetId);

        return ok(resultset.render(loggedInUser, r));
    }

    public static Result visualizeTimeSeries(Long timeSeriesId) {
        User loggedInUser = User.findByEmail(request().username());
        TimeSeries timeSeries = TimeSeries.findById(timeSeriesId);

        return ok(timeseries.render(loggedInUser, timeSeries));
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

    public static Result resultssetall(Long id) {
        ResultSet r = ResultSet.findById(id);
        ObjectNode result = Json.newObject();
        result.put("id", id);
        result.put("name", r.name);
        result.put("desc", r.description);
        result.put("uploaded", User.findById(r.uploaderId).email);
        if(r.timeSeriesId != null){
            result.put("timeSeriesId", r.timeSeriesId);
            result.put("timeSeriesSeqNumber", r.timeSeriesSeqNumber);
        }
        List<Cluster> clusters = Cluster.findByResultSet(r.id);
        List<ObjectNode> clusterjsons = new ArrayList<ObjectNode>();
        for (int i = 0; i < clusters.size(); i++) {
            ObjectNode cluster = Json.newObject();
            cluster.put("clusterid", clusters.get(i).cluster);
            cluster.put("points", Json.toJson(Point.findByCluster(id, clusters.get(i).id)));
            clusterjsons.add(cluster);
        }

        result.put("clusters", Json.toJson(clusterjsons));
        return ok(result);
    }

    public static Result timeseries(Long id) {
        TimeSeries timeSeries = TimeSeries.findById(id);
        ObjectNode result = Json.newObject();
        result.put("id", id);
        result.put("name", timeSeries.name);
        result.put("desc", timeSeries.description);
        result.put("uploaded", User.findById(timeSeries.uploaderId).email);

        List<ResultSet> resultSets = ResultSet.findByTimeSeriesId(id);
        result.put("resultsets", Json.toJson(resultSets));
        return ok(result);
    }

    public static Result resultset(Long id) {
        ResultSet r = ResultSet.findById(id);
        ObjectNode result = Json.newObject();
        result.put("id", id);
        result.put("name", r.name);
        result.put("desc", r.description);
        result.put("uploaded", User.findById(r.uploaderId).email);

        List<Cluster> clusters = Cluster.findByResultSet(r.id);

        result.put("clusters", Json.toJson(clusters));

        return ok(result);
    }

    public static Result cluster(Long rid, Integer cid) {
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
