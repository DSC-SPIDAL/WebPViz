package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import db.Constants;
import db.ExperimentDAO;
import models.User;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

public class ExperimentController extends Controller {
    @Security.Authenticated(Secured.class)
    public static Result getExperiment(int artifactId) {
        User loggedInUser = User.findByEmail(request().username());
        String comment = ExperimentDAO.getExperiment(artifactId, loggedInUser.email);
        if (comment != null) {
            return ok(comment).as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result updateExperiment() {
        User loggedInUser = User.findByEmail(request().username());
        JsonNode json = request().body().asJson();
        JsonNode expNode = json.get(Constants.Experiment.EXP);
        String body = Json.stringify(expNode);
        int timeSeriesId = 0;
        String timeSeries = json.get(Constants.Experiment.TIME_SERIES_ID_FIELD).asText();
        if (timeSeries != null) {
            timeSeriesId = Integer.parseInt(timeSeries);
            ExperimentDAO.updateExperiment(timeSeriesId, loggedInUser.email, body);
            return ok("{status: 'success'}").as("application/json");
        } else {
            return badRequest("{status: 'fail'}").as("application/json");
        }
    }
}
