/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import db.MongoConnection;
import models.User;
import models.utils.AppException;
import org.pac4j.core.client.Clients;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.play.Config;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

public class Global extends GlobalSettings{

    @Override
    public void onStart(Application application) {
        super.onStart(application);
        // Add admin users
        try {
            registerAdmins();
        } catch (AppException e) {
            Logger.error("Cannot register admin users.", e);
            throw new RuntimeException("Something went wrong during startup", e);
        }

        // create the group dao
        MongoConnection con = MongoConnection.getInstance();
        con.initGroupsCollection();
        fbLoging();
    }

    private void registerAdmins() throws AppException {
        String email = "webplotviziu";
        String password = "webplotviziu";

        if (User.findByEmail(email) == null) {
            User.create(email, password);
        } else {
            Logger.warn(String.format("User with email %s already exists.", email));
        }
    }

    private void fbLoging() {
        final String fbId = Play.application().configuration().getString("fbId");
        final String fbSecret = Play.application().configuration().getString("fbSecret");
        final String baseUrl = Play.application().configuration().getString("baseUrl");

        // OAuth
        final FacebookClient facebookClient = new FacebookClient(fbId, fbSecret);

        final Clients clients = new Clients(baseUrl + "/callback", facebookClient); // , casProxyReceptor);
        Config.setClients(clients);
    }
}
