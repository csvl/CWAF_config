package be.uclouvain.service;

import java.util.*;

import org.apache.jena.ontology.Individual;

public class CompileContext implements Cloneable{

    private Stack<Individual> trace = new Stack<>();
    private Map<String, String> localVars = new HashMap<>();
    private Map<String, List<String>> varTag = new HashMap<>();
    

    public CompileContext(){}

    public CompileContext(CompileContext other){
        trace.addAll(other.trace);
        localVars.putAll(other.localVars);
        for (Map.Entry<String, List<String>> entry : other.varTag.entrySet()) {
            List<String> copy = new ArrayList<>(entry.getValue());
            varTag.put(entry.getKey(), copy);
        }
    }

    public void push(Individual directive) {
        trace.push(directive);
    }

    public Individual pop() {
        return trace.pop();
    }

    public void addVar(String name, String value){
        localVars.put(name, value);
    }

    public void addVar(String name, String value, String tag){
        localVars.put(name, value);
        List<String> tagList = varTag.getOrDefault(tag, new ArrayList<>());
        tagList.add(name);
        varTag.put(tag, tagList);
    }

    public void removeTaggedVar(String tag) {
        List<String> taggedVars = varTag.get(tag);
        for (String var : taggedVars) {
            localVars.remove(var);
        }
    }

    public Map<String, String> getLocalVars() {
        return localVars;
    }

    public Stack<Individual> getTrace() {
        // Stack<Individual> copy = new Stack<>();
        // copy.addAll(trace);
        // return copy;
        return trace;
    }

    @Override
    public String toString() {
        return "CompileContext{" +
                "trace=" + trace +
                ", localVars=" + localVars +
                '}';
    }

}
