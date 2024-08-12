package be.uclouvain.model;

import java.util.regex.Pattern;


public class Location {

    private String path;

    public Location(String path) {
        this.path = path;
    }

    public boolean match(String path) {
        if (path.isEmpty()) {
            path = "/";
        }
        if (this.path == null) {
            return true;
        }
        if (this.path.startsWith("~")) {
            //check if match regex
            String regex = this.path.substring(1).strip().replaceAll("^[\"\']|[\"\']$", "");
            Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(path).find();
        }
        return path.startsWith(this.path) || path.startsWith(path + "/");
    }
    
}
