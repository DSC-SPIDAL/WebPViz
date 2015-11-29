package models;

public class Group {
    public String name;
    public String description;

    public Group() {
    }

    public Group(String name) {
        this.name = name;
    }

    public Group(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
