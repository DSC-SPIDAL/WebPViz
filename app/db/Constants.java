package db;

public class Constants {
    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String UPLOADER_ID_FIELD = "uploaderId";
    public static final String FILE_NAME_FIELD = "fileName";
    public static final String TIME_SERIES_SEQ_NUMBER_FIELD = "timeSeriesSeqNumber";
    public static final String TIME_SERIES_ID_FIELD = "timeSeriesId";
    public static final String DESC_FIELD = "desc";
    public static final String UPLOADED_FIELD = "uploaded";
    public static final String LABEL_FIELD = "label";
    public static final String STATUS_FIELD = "status";
    public static final String RESULTSETS_FIELD = "resultsets";
    public static final String DATE_CREATION_FIELD = "dateCreation";
    public static final String COLOR_FIELD = "color";
    public static final String SIZE_FIELD = "size";
    public static final String VISIBLE_FIELD = "visible";
    public static final String SHAPE_FIELD = "shape";
    public static final String CLUSTERID_FIELD = "clusterid";
    public static final String STATUS_PENDING = "pending";

    public static class Group {
        public static final String DEFAULT_GROUP = "default";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "desc";
    }

    public static class DB {
        public static final String MONGO_HOST = "mongo.host";
        public static final String MONGO_PORT = "mongo.port";
        public static final String FILES_COLLECTION = "files";
        public static final String CLUSTERS_COLLECTION = "clusters";
        public static final String DB_NAME = "pviz";
        public static final String GROUPS_COLLECTION = "groups";
    }
}
