package models;

public class Tag {
    public String name;
    public String description;
    public String category;
    public String userId;
    public boolean pub = false;

    public Tag(){

    }

    public Tag(String userId, String name){
        this.userId = userId;
        this.name = name;
        this.category = "default";
    }

    public Tag(String userId, String name, String description) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.category = "default";
    }

    public Tag(String userId, String name, String description, boolean pub) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.pub = pub;
        this.category = "default";
    }

    public Tag(String userId, String name, String description,String category, boolean pub) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.pub = pub;
        this.category = category;
    }

}
