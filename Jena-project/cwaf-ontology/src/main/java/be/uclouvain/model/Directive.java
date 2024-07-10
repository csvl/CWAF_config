package be.uclouvain.model;

import static be.uclouvain.utils.DirectiveFactory.parseArguments;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.jena.ontology.Individual;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import be.uclouvain.service.CompileContext;
import be.uclouvain.service.Constants;
import be.uclouvain.vocabulary.OntCWAF;

public class Directive implements Comparable<Directive>, Serializable {

    private int lineNum;
    private transient Individual resource;
    private String location = "global";
    private String virtualHost;
    private int ifLevel = 0;
    private int phase = Constants.DEFAULT_PHASE; 
    private String[] args;
    private Condition existance_condition;
    private String name;

    public Directive(CompileContext ctx, Individual resource) {

        this.lineNum = resource.getPropertyValue(OntCWAF.DIR_LINE_NUM).asLiteral().getInt();
        // Statement scope = resource.getProperty(OntCWAF.HAS_SCOPE);
        // if (scope != null) {
        //     Individual scopeInd = scope.getObject().as(Individual.class);
        //     Statement location = scopeInd.getProperty(OntCWAF.HAS_LOCATION);
        //     this.location = location != null ? location.getObject().as(Individual.class)
        //                             .getPropertyValue(OntCWAF.LOCATION_PATH).asLiteral().getString() : "global";
        //     Statement virtualHost = scopeInd.getProperty(OntCWAF.HAS_VIRTUAL_HOST);
        //     this.virtualHost = virtualHost != null ? virtualHost.getObject().as(Individual.class)
        //                             .getPropertyValue(OntCWAF.V_HOST_NAME).asLiteral().getString() : null;
        // }

        this.name = resource.getLocalName();

        this.location = ctx.getCurrentLocation();
        this.virtualHost = ctx.getCurrentVirtualHost();

        if (resource.hasOntClass(OntCWAF.MOD_SEC_RULE)) {
            if (resource.hasProperty(OntCWAF.PHASE)) {
                this.phase = resource.getPropertyValue(OntCWAF.PHASE).asLiteral().getInt();
            } else {
                System.err.println("Cannot retrieve phase level for directive " + resource.getLocalName());
            }
        }

        RDFNode argNode = resource.getPropertyValue(OntCWAF.ARGUMENTS);
        String argsString = argNode != null ? argNode.asLiteral().getString() : "";
        this.args = parseArguments(argsString, null);

        this.existance_condition = ctx.getEC();

        ctx.getTrace().forEach( ancestor -> {
            if (ancestor.hasOntClass(OntCWAF.IF_FAMILY) || ancestor.hasOntClass(OntCWAF.ELSE_FAMILY)) {
                ifLevel++;
            }
        });
        this.resource = resource;
    }

    // public String[] getScope() {
    //     return new String[] {location, virtualHost};
    // }

    // public void setScope(String[] scope) {
    //     if (scope.length != 2) {
    //         throw new IllegalArgumentException("Scope must have 2 elements");
    //     }
    //     this.location = scope[0];
    //     this.virtualHost = scope[1];
    //     // if (virtualHost != null || !location.equals("global")) {
    //     //     System.err.println("Scope set to " + Arrays.toString(scope));
    //     // }
    // }

    public boolean isBeacon() {
        return resource.hasOntClass(OntCWAF.BEACON);
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public void setArgs(String arg, int i) {
        this.args[i] = arg;
    }

    public int getPhase() {
        return phase;
    }

    public int getLineNum() {
        return lineNum;
    }

    public Individual getIndividual() {
        return resource;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Directive other = (Directive) obj;
        return phase == other.phase &&
                ifLevel == other.ifLevel &&
                lineNum == other.lineNum &&
                (virtualHost != null ? virtualHost.equals(other.virtualHost) : other.virtualHost == null) &&
                (location != null ? location.equals(other.location) : other.location == null);
    }

    @Override
    // WARNING: The comparison only make sense if the directives are from the same file
    public int compareTo(Directive other) {
        if (this.phase != other.phase) return Integer.compare(this.phase, other.phase);
        if (this.ifLevel != other.ifLevel) return Integer.compare(this.ifLevel, other.ifLevel);
        if (this.location != null && other.location == null) return 1;
        if (this.location == null && other.location != null) return -1;
        if (this.virtualHost != null && other.virtualHost == null) return 1;
        if (this.virtualHost == null && other.virtualHost != null) return -1;
        return Integer.compare(this.lineNum, other.lineNum);
    }

    @Override
    public String toString() {
        String EC = existance_condition.getCondition();
        return "{" +
                "phase=" + phase +
                ", ifLevel=" + ifLevel +
                ", location='" + (location == null ? "global" : location) + '\'' +
                ", virtualHost='" + (virtualHost== null ? "" : virtualHost) + '\'' +
                ", lineNum=" + lineNum +
                (EC == "true" ? "" : ", EC=" + existance_condition.getCondition()) +
                "}\t" + name + "\t(" + String.join(" ", args) + ")";
    }

    public void setPhase(int newPhase) {
        this.phase = newPhase;
    }
}
