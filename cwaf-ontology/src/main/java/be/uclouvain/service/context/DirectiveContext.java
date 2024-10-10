package be.uclouvain.service.context;

import java.net.URL;
import java.util.Stack;

public class DirectiveContext {
    
    public String currentVirtualhost = "";
    public String currentLocation = "";
    public Stack<String> beaconStack = new Stack<>();
    public String lastIf = "";
    public String serverName = "";
    public String serverPort = "0";
    public URL url;

    public DirectiveContext(URL url) {
        this.url = url;
    }

    public boolean isMatchingURL(){
        if (url == null) {
            return true;
        }
        int port = -1;
        try {
            port = Integer.parseInt(serverPort);
        } catch (Exception e) {
            return false;
        }
        boolean checkHost = url.getHost().equals(currentVirtualhost) || currentVirtualhost.equals("*") || currentVirtualhost.equals("");
        boolean checkLocation = url.getPath().equals(currentLocation) || currentLocation.equals(""); 
        boolean checkPort = url.getPort() == port || port == 0;
        return checkHost && checkLocation && checkPort;
    }
    
}
