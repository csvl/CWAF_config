package be.uclouvain.model;

public class LocalVar {
    public String name = "";
    public String value = "";
    public String tag = "";

    public LocalVar(String name, String value, String tag) {
        this.name = name;
        this.value = value;
        this.tag = tag;
    }

    public LocalVar(String name, String value) {
        this.name = name;
        this.value = value;
        this.tag = "";
    }
}
