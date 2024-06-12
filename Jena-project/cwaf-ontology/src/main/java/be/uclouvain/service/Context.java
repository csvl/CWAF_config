package be.uclouvain.service;

import java.util.Stack;

public class Context {
    
    public String currentVirtualhost = "";
    public String currentLocation = "";
    public Stack<String> currentIfStack = new Stack<>();
    public int currentOrderingNumber = 0;
    public Stack<String> currentMacroStack = new Stack<>();
    public Integer currentInstructionNumber = null;
    
}
