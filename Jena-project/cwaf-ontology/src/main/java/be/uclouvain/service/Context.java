package be.uclouvain.service;

import java.util.Stack;

public class Context {
    
    public String currentVirtualhost = "";
    public String currentLocation = "";
    public Stack<String> beaconStack = new Stack<>();
    // public Integer currentInstructionNumber = null;
    public String serverName = "";
    public int serverPort = 0;
    
}
