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

import com.opencsv.CSVReader;
import models.xml.PVizPoint;
import models.xml.Plotviz;
import models.xml.XMLLoader;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.*;
import java.util.Date;
import java.util.List;

@Entity
public class ResultSet extends Model {
    @Id
    public Integer id;

    @Constraints.Required
    public String name;

    @Constraints.Required
    public String description;

    @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date dateCreation;

    @Constraints.Required
    @Formats.NonEmpty
    public Integer uploaderId;

    @Constraints.Required
    public Integer timeSeriesId;

    public Integer timeSeriesSeqNumber;

    public String fileName;

    public static Model.Finder<Integer, ResultSet> find = new Model.Finder<Integer, ResultSet>(Integer.class, ResultSet.class);

    public static ResultSet create(String name, String description, User uploader, String fileName) {
        ResultSet r = new ResultSet();
        r.name = name;
        r.description = description;
        r.uploaderId = uploader.id;
        r.dateCreation = new Date();
        r.fileName = fileName;

        r.save();

        return r;
    }

    public static ResultSet create(String name, String description, User uploader, TimeSeries timeSeries, Integer sequenceNumber, String fileName) {
        ResultSet r = new ResultSet();
        r.name = name;
        r.description = description;
        r.uploaderId = uploader.id;
        r.dateCreation = new Date();
        r.timeSeriesId =  timeSeries.id;
        r.timeSeriesSeqNumber =  sequenceNumber;
        r.fileName = fileName;

        r.save();

        return r;
    }

    public static ResultSet createFromFile(String name, String description, User uploader, File file, String originalFileName) throws IOException {
        // first lets try to load as XML
        try {
            XMLLoader.load(new FileInputStream(file));
            return createFromXMLFile(name, description, uploader, new FileInputStream(file), originalFileName);
        } catch (Exception ignore) {
        }

        // now lets try CSV format
        try {
            ResultSet r = create(name, description, uploader, originalFileName);
            CSVReader reader = new CSVReader(new FileReader(file), '\t');

            String[] record;
            while ((record = reader.readNext()) != null) {
                Integer clusterId = Integer.valueOf(record[4].trim());
                Cluster c = Cluster.findByClusterId(r.id, clusterId);
                if (c == null) {
                    c = Cluster.create(clusterId, r);
                }

                Long pId = Long.valueOf(record[0]);
                Float x = Float.valueOf(record[1]);
                Float y = Float.valueOf(record[2]);
                Float z = Float.valueOf(record[3]);

                Point.create(x, y, z, c, r);
            }
            return r;
        } catch (IOException e) {
            throw new IOException("Failed to load file", e);
        }
    }

    public static ResultSet createFromFile(String name, String description, User uploader, File file,
                                           TimeSeries timeSeries, Integer sequenceNumber, String originalFileName) throws IOException {
        ResultSet r = create(name, description, uploader, timeSeries,sequenceNumber, originalFileName);
        CSVReader reader = new CSVReader(new FileReader(file), '\t');
        String[] record;
        while ((record = reader.readNext()) != null) {
            Integer clusterId = Integer.valueOf(record[4].trim());
            Cluster c = Cluster.findByClusterId(r.id, clusterId);
            if (c == null) {
                c = Cluster.create(clusterId, r);
            }

            Long pId = Long.valueOf(record[0]);
            Float x = Float.valueOf(record[1]);
            Float y = Float.valueOf(record[2]);
            Float z = Float.valueOf(record[3]);

            Point.create(x, y, z, c, r);
        }

        return r;
    }

    public static ResultSet createFromXMLFile(String name, String description, User uploader, InputStream file, String originalFileName) throws Exception {
        return createFromXMLFile(name, description, uploader, file, null, null, null);
    }

    public static ResultSet createFromXMLFile(String name, String description, User uploader, InputStream file,
                                              TimeSeries timeSeries, Integer sequenceNumber, String originalFileName) throws Exception {
        ResultSet r;
        if (timeSeries != null) {
            r = create(name, description, uploader, timeSeries, sequenceNumber, originalFileName);
        } else {
            r = create(name, description, uploader, originalFileName);
        }
        Plotviz plotviz = XMLLoader.load(file);
        List<models.xml.Cluster> clusters = plotviz.getClusters();
        for (models.xml.Cluster cl : clusters) {
            Cluster c = Cluster.findByClusterId(r.id, cl.getKey());
            if (c == null) {
                c = Cluster.create(cl.getKey(), r);
            }
            c.color = new Color(cl.getColor().getA(), cl.getColor().getB(), cl.getColor().getG(), cl.getColor().getR());
            c.label = cl.getLabel();
            c.size = cl.getSize();
            c.visible = cl.getVisible();
            c.shape = cl.getShape();

            c.save();
        }

        List<PVizPoint> points = plotviz.getPoints();
        for (int i = 0; i < points.size(); i++) {
            PVizPoint point = points.get(i);
            int clusterkey = point.getClusterkey();
            Cluster c = Cluster.findByClusterId(r.id, clusterkey);
            if (c == null) {
                c = Cluster.create(clusterkey, r);
            }
            Point.create(point.getLocation().getX(), point.getLocation().getY(), point.getLocation().getZ(), c, r);
        }

        return r;
    }

    public static ResultSet findById(int id) {
        return find.byId(id);
    }

    public static ResultSet findByName(String name) {
        return find.where().eq("name", name).findUnique();
    }

    public static List<ResultSet> findByTimeSeriesId(Long id) { return find.where().eq("timeSeriesId", id).findList();}

    public static List<ResultSet> all() {
        return find.all();
    }

}
