package be.uclouvain.service;

import java.util.*;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import be.uclouvain.model.Condition;
import be.uclouvain.model.Scope;

public class CompileContext{

    private OntModel model;
    private OntModel schema;
    private OntModel infModel;
    private Stack<Individual> trace = new Stack<>();
    private Map<String, String> localVars = new HashMap<>();
    private Map<String, List<String>> varTag = new HashMap<>();
    private Stack<Condition> existance_conditions = new Stack<>();
    private Scope scope = new Scope();
    private List<String> definedMacros = new ArrayList<>();
    

    public CompileContext(){}

    public CompileContext(OntModel model, OntModel schema){
        this.model = ModelFactory.createOntologyModel();
        this.model.add(model);
        this.model.add(schema);
        this.schema = schema;
        this.infModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        this.infModel.add(model);
        this.infModel.add(schema);
    }

    public CompileContext(CompileContext other){
        model = other.model;
        schema = other.schema;
        infModel = other.infModel;

        scope = other.scope;
        definedMacros.addAll(other.definedMacros);

        trace.addAll(other.trace);
        localVars.putAll(other.localVars);
        for (Map.Entry<String, List<String>> entry : other.varTag.entrySet()) {
            List<String> copy = new ArrayList<>(entry.getValue());
            varTag.put(entry.getKey(), copy);
        }
        existance_conditions.addAll(other.existance_conditions);
    }

    public OntModel getModel() {
        return model;
    }

    public OntModel getSchema() {
        return schema;
    }

    public OntModel getInfModel() {
        return infModel;
    }

    public void setModel(OntModel model) {
        this.model = model;
    }

    public void setSchema(OntModel schema) {
        this.schema = schema;
        this.infModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        this.infModel.add(model);
        this.infModel.add(schema);
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

    public void defineMacro(String name) {
        definedMacros.add(name);
    }

    public boolean isMacroDefined(String name) {
        return definedMacros.contains(name);
    }

    public void undefineMacro(String name) {
        if (!definedMacros.remove(name)) {
            System.err.println("Attempt to undefine macro " + name + ", which was not defined");
        }
    }

    public void addVar(String name, String value, String tag){
        localVars.put(name, value);
        List<String> tagList = varTag.getOrDefault(tag, new ArrayList<>());
        tagList.add(name);
        varTag.put(tag, tagList);
    }

    public void removeVar(String name){
        localVars.remove(name);
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

    public String getCurrentVirtualHost() {
        return scope.currentVirtualHost;
    }

    public void setCurrentVirtualHost(String currentVirtualHost) {
        this.scope.currentVirtualHost = currentVirtualHost;
    }

    public void resetCurrentVirtualHost() {
        this.scope.currentVirtualHost = null;
    }

    public String getCurrentLocation() {
        return scope.currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.scope.currentLocation = currentLocation;
    }

    public void resetCurrentLocation() {
        this.scope.currentLocation = null;
    }

    @Override
    public String toString() {
        return "CompileContext{" +
                ", trace=" + Arrays.toString(trace.toArray()) +
                ", localVars=" + localVars +
                ", varTag=" + varTag +
                ", existance_conditions=" + Arrays.toString(existance_conditions.toArray()) +
                ", currentVirtualHost='" + scope.currentVirtualHost + '\'' +
                ", currentLocation='" + scope.currentLocation + '\'' +
                '}';
    }

}
