package controllers;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.play.java.JavaController;
import org.pac4j.play.java.RequiresAuthentication;
import play.mvc.Result;
import views.html.protectedIndex;

public class LoggingController extends JavaController {
    private static Result protectedIndex() {
        // profile
        final CommonProfile profile = getUserProfile();
        return ok(protectedIndex.render(profile));
    }

    @RequiresAuthentication(clientName = "FacebookClient")
    public static Result facebookIndex() {
        return protectedIndex();
    }
}
