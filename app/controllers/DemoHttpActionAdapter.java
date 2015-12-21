package controllers;

import org.pac4j.core.context.WebContext;
import org.pac4j.play.PlayWebContext;
import org.pac4j.core.http.HttpActionAdapter;
import play.mvc.Result;

import static play.mvc.Results.*;

public class DemoHttpActionAdapter implements HttpActionAdapter {

    @Override
    public Object adapt(int i, WebContext webContext) {
        return null;
    }
}