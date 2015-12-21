package controllers;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.play.java.JavaController;
import org.pac4j.play.java.RequiresAuthentication;
import play.mvc.Result;

public class LoggingController extends JavaController {
    private static Result protectedIndex() {
        // profile
        final CommonProfile profile = getUserProfile();
        return ok(views.html.protectedIndex.render(profile));
    }

    @RequiresAuthentication(clientName = "OidcClient")
    public static Result oidcIndex() {
        return protectedIndex();
    }
}
