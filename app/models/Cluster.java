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

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.List;

public class Cluster extends Model {

    @Id
    public Integer id;

    @Constraints.Required
    public Integer cluster;

    @Constraints.Required
    @Formats.NonEmpty
    public Integer resultSet;

    public int size;

    @OneToOne(cascade= CascadeType.ALL)
    public Color color;

    public String shape;

    public int visible;

    public String label;

    public static Model.Finder<Integer, Cluster> find = new Model.Finder<Integer, Cluster>(Integer.class, Cluster.class);


    public static Cluster create(Integer cluster, ResultSet resultSet) {
        Cluster c = new Cluster();
        c.cluster = cluster;
        c.resultSet = resultSet.id;

        c.save();

        return c;
    }

    public static Cluster findByClusterId(Integer rId, Integer cluster) {
        return find.where().eq("cluster", cluster).eq("resultSet", rId).findUnique();
    }

    public static List<Cluster> findByResultSet(Integer id) {
        return find.where().eq("resultSet", id).findList();
    }
}
