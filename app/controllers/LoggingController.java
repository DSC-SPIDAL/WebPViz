package controllers;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.play.java.RequiresAuthentication;
import org.pac4j.play.java.UserProfileController;
import play.mvc.Result;

public class LoggingController extends UserProfileController<CommonProfile> {
    private Result protectedIndex() {
        // profile
        final CommonProfile profile = getUserProfile();
        return ok(views.html.protectedIndex.render(profile));
    }

    @RequiresAuthentication(clientName = "OidcClient")
    public Result oidcIndex() {
        return protectedIndex();
    }

}
