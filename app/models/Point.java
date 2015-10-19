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

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

@Entity
public class Point extends Model{

    @Id
    public Long id;

    @Constraints.Required
    public Float x;

    @Constraints.Required
    public Float y;

    @Constraints.Required
    public Float z;

    @Constraints.Required
    public Long cluster;

    @Constraints.Required
    public Long resultSet;

    public static Model.Finder<Long, Point> find = new Model.Finder<Long, Point>(Long.class, Point.class);

    public static Point create( Float x, Float y, Float z, Cluster cluster, ResultSet resultSet){
        Point p = new Point();
        p.x = x;
        p.y = y;
        p.z = z;
        p.cluster = cluster.id;
        p.resultSet = resultSet.id;

        p.save();

        return p;
    }

    public static List<Point> findByCluster(Long rid, Long cid){
        return find.where().eq("resultSet", rid).eq("cluster", cid).findList();
    }
}
