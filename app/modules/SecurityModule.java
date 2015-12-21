package modules;

import com.google.inject.AbstractModule;
import controllers.CustomAuthorizer;
import controllers.DemoHttpActionAdapter;
import org.pac4j.cas.client.CasClient;
import org.pac4j.core.authorization.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.play.ApplicationLogoutController;
import org.pac4j.play.CallbackController;
import org.pac4j.play.cas.logout.PlayCacheLogoutHandler;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import play.Configuration;
import play.Environment;

import java.io.File;

public class SecurityModule extends AbstractModule {

    public final static String JWT_SALT = "12345678901234567890123456789012";

    private final Environment environment;
    private final Configuration configuration;

    public SecurityModule(
            Environment environment,
            Configuration configuration) {
        this.environment = environment;
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        final String baseUrl = configuration.getString("baseUrl");

        // OpenID Connect
        final OidcClient oidcClient = new OidcClient();
        oidcClient.setClientID("343992089165-i1es0qvej18asl33mvlbeq750i3ko32k.apps.googleusercontent.com");
        oidcClient.setSecret("unXK_RSCbCXLTic2JACTiAo9");
        oidcClient.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration");
        oidcClient.addCustomParam("prompt", "consent");


        // basic auth
        final DirectBasicAuthClient directBasicAuthClient = new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        final Clients clients = new Clients(baseUrl + "/callback", oidcClient, directBasicAuthClient); // , casProxyReceptor);

        final Config config = new Config(clients);
        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        config.addAuthorizer("custom", new CustomAuthorizer());
        config.setHttpActionAdapter(new DemoHttpActionAdapter());
        bind(Config.class).toInstance(config);

        // for test purposes: profile timeout = 60 seconds
        //((PlayCacheStore) config.getSessionStore()).setProfileTimeout(60);

        // callback
        final CallbackController callbackController = new CallbackController();
        callbackController.setDefaultUrl("/");
        bind(CallbackController.class).toInstance(callbackController);
//        // logout
        final ApplicationLogoutController logoutController = new ApplicationLogoutController();
        logoutController.setDefaultUrl("/");
        bind(ApplicationLogoutController.class).toInstance(logoutController);
    }
}