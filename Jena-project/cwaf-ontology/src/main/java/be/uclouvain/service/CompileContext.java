package be.uclouvain.service;

import java.util.*;

import org.apache.jena.ontology.Individual;

import be.uclouvain.model.Condition;

public class CompileContext{

    private Stack<Individual> trace = new Stack<>();
    private Map<String, String> localVars = new HashMap<>();
    private Map<String, List<String>> varTag = new HashMap<>();
    private Stack<Condition> existance_conditions = new Stack<>();
    

    public CompileContext(){}

    public CompileContext(CompileContext other){
        trace.addAll(other.trace);
        localVars.putAll(other.localVars);
        for (Map.Entry<String, List<String>> entry : other.varTag.entrySet()) {
            List<String> copy = new ArrayList<>(entry.getValue());
            varTag.put(entry.getKey(), copy);
        }
        existance_conditions.addAll(other.existance_conditions);
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

    public void addEC(String condition) {
        existance_conditions.push(new Condition(condition));
    }

    public void addEC(Condition condition) {
        existance_conditions.push(condition);
    }

    public Condition popEC() {
        return existance_conditions.pop();
    }

    public Condition getEC() {
        return existance_conditions.stream().reduce((C1, C2) -> C1.and(C2)).orElse(new Condition("true"));
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
