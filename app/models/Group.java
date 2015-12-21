package models;

public class Group {
    public String name;
    public String description;
    public int userId;
    public boolean pub = false;

    public Group() {
    }

    public Group(int userId, String name) {
        this.name = name;
        this.userId = userId;
    }

    public Group(int userId, String name, String description) {
        this.userId = userId;
        this.name = name;
        this.description = description;
    }

    public Group(int userId, String name, String description, boolean pub) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.pub = pub;
    }
}
