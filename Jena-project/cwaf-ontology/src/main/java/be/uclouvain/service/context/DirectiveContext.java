package be.uclouvain.service.context;

import java.util.Stack;

public class DirectiveContext {
    
    public String currentVirtualhost = "";
    public String currentLocation = "";
    public Stack<String> beaconStack = new Stack<>();
    public String lastIf = "";
    // public Integer currentInstructionNumber = null;
    public String serverName = "";
    public int serverPort = 0;
    
}
