package db;

public class Constants {
    public static final String UPLOADER_ID_FIELD = "uploaderId";
    public static final String LABEL_FIELD = "label";
    public static final String COLOR_FIELD = "color";
    public static final String SIZE_FIELD = "size";
    public static final String VISIBLE_FIELD = "visible";
    public static final String SHAPE_FIELD = "shape";
    public static final String CLUSTERID_FIELD = "clusterid";

    public static class ArtifactType {
        public static final String TIME_SERIES = "T";
        public static final String PLOTVIZ = "P";
        public static final String POINT = "S";
    }

    public static class ArtifactStatus {
        public static final String ACTIVE = "active";
        public static final String PENDING = "pending";
    }

    public static class File {
        public static final String TIME_SERIES_SEQ_NUMBER_FIELD = "seq";
        public static final String FILE_NAME_FIELD = "file";
        public static final String CLUSTERS = "clusters";
        public static final String EDGES = "edges";
        public static final String POINTS = "points";
        public static final String TIME_SERIES_ID_FIELD = "tId";
    }

    public static class Cluster {
        public static final String SHAPE = "f";
        public static final String SIZE = "s";
        public static final String VISIBILE = "v";
        public static final String COLOR = "r";
        public static final String POINTS = "p";
        public static final String KEY = "k";
        public static final String LABEL = "l";
    }

    public static class Edge {
        public static final String ID = "i";
        public static final String COLOR = "r";
        public static final String VERTICES = "v";
    }

    public static class Point {
        public static final String KEY = "k";
        public static final String CLUSTER = "c";
        public static final String VALUE = "v";
        public static final String ID_FIELD = "id";
        public static final String SETTINGS = "settings";
        public static final String UDP = "udp";
    }

    public static class Artifact {
        public static final String ID_FIELD = "id";
        public static final String NAME_FIELD = "name";
        public static final String DESCRIPTION_FIELD = "desc";
        public static final String USER = "uploader";
        public static final String DATE_CREATION_FIELD = "dateCreation";
        public static final String GROUP_FIELD = "group";
        public static final String DESC_FIELD = "desc";
        public static final String STATUS_FIELD = "status";
        public static final String TYPE = "type";  // 1: time series,2: ploviz,3: text
        public static final String FILE_NAME_FIELD = "fileName";
        public static final String VERSION = "version";
        public static final String SETTINGS = "settings";
        public static final String UDP = "udp";
        public static final String FILES = "files";
        public static final String PUBLIC = "public";
        public static final String PUBLIC_TRUE = "t";
        public static final String PUBLIC_FALSE = "f";
    }

    public static class Group {
        public static final String DEFAULT_GROUP = "default";
        public static final String NAME = "name";
        public static final String USER = "user";
        public static final String DESCRIPTION = "desc";
    }

    public static class DB {
        public static final String MONGO_HOST = "mongo.host";
        public static final String MONGO_PORT = "mongo.port";
        public static final String ARTIFACTS_COLLECTION = "artificats";
        public static final String FILES_COLLECTION = "files";
        public static final String DB_NAME = "pviz2";
        public static final String GROUPS_COLLECTION = "groups";
    }
}
