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
package models;

import models.utils.AppException;
import models.utils.Hash;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class User extends Model {
    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String email;

    @Constraints.Required
    @Formats.NonEmpty
    public String passwordHash;

    // -- Queries (long id, user.class)
    public static Model.Finder<Integer, User> find = new Model.Finder<Integer, User>(Integer.class, User.class);

    public static User findById(Integer id){
        return find.byId(id);
    }
    /**
     * Retrieve a user from an email.
     *
     * @param email email to search
     * @return a user
     */
    public static User findByEmail(String email) {
        return find.where().eq("email", email).findUnique();
    }


    /**
     * Authenticate a User, from a email and clear password.
     *
     * @param email         email
     * @param clearPassword clear password
     * @return User if authenticated, null otherwise
     * @throws AppException App Exception
     */
    public static User authenticate(String email, String clearPassword) throws AppException {
        // get the user with email only to keep the salt password
        User user = find.where().eq("email", email).findUnique();
        if (user != null) {
            // get the hash password from the salt + clear password
            if (Hash.checkPassword(clearPassword, user.passwordHash)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Creates a user
     *
     * @param email         email
     * @param clearPassword clear text password
     * @return User if successful, null otherwise
     * @throws AppException App exception
     */
    public static User create(String email, String clearPassword) throws AppException {
        User u = new User();
        u.email = email;
        u.passwordHash = Hash.createPassword(clearPassword);
        u.save();

        return u;
    }

}
