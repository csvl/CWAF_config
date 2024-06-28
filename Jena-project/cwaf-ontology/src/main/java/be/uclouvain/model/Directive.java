package be.uclouvain.model;

import java.util.Arrays;

import org.apache.jena.ontology.Individual;
import org.apache.jena.rdf.model.Statement;

import be.uclouvain.service.CompileContext;
import be.uclouvain.service.Constants;
import be.uclouvain.vocabulary.OntCWAF;

public class Directive implements Comparable<Directive> {
    
    private int lineNum;
    private Individual resource;
    private String location = "global";
    private String virtualHost;
    private int ifLevel = 0;
    private int phase = Constants.DEFAULT_PHASE; 

    public Directive(CompileContext ctx, Individual resource) {
        this.lineNum = resource.getPropertyValue(OntCWAF.DIR_LINE_NUM).asLiteral().getInt();
        Statement scope = resource.getProperty(OntCWAF.HAS_SCOPE);
        if (scope != null) {
            Individual scopeInd = scope.getObject().as(Individual.class);
            Statement location = scopeInd.getProperty(OntCWAF.HAS_LOCATION);
            this.location = location != null ? location.getObject().as(Individual.class)
                                    .getPropertyValue(OntCWAF.LOCATION_PATH).asLiteral().getString() : "global";
            Statement virtualHost = scopeInd.getProperty(OntCWAF.HAS_VIRTUAL_HOST);
            this.virtualHost = virtualHost != null ? virtualHost.getObject().as(Individual.class)
                                    .getPropertyValue(OntCWAF.V_HOST_NAME).asLiteral().getString() : null;
        }
        
        if (resource.hasOntClass(OntCWAF.MOD_SEC_RULE)) {
            if (resource.hasProperty(OntCWAF.PHASE)) {
                this.phase = resource.getPropertyValue(OntCWAF.PHASE).asLiteral().getInt();
            } else {
                System.err.println("Cannot retrieve phase level for directive " + resource.getLocalName());
            }
        }

        ctx.getTrace().forEach( ancestor -> {
            if (ancestor.hasOntClass(OntCWAF.IF) || ancestor.hasOntClass(OntCWAF.ELSE_IF) || ancestor.hasOntClass(OntCWAF.ELSE)) {
                ifLevel++;
            }
        });
        this.resource = resource;
    }

    public String[] getScope() {
        return new String[] {location, virtualHost};
    }

    public void setScope(String[] scope) {
        if (scope.length != 2) {
            throw new IllegalArgumentException("Scope must have 2 elements");
        }
        this.location = scope[0];
        this.virtualHost = scope[1];
    }

    public boolean isBeacon() {
        return resource.hasOntClass(OntCWAF.BEACON);
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
        return resource.getLocalName() + "{" +
                "phase=" + phase +
                ", ifLevel=" + ifLevel +
                ", location='" + location + '\'' +
                ", virtualHost='" + virtualHost + '\'' +
                ", lineNum=" + lineNum +
                "}";
    }

    public void setPhase(int newPhase) {
        this.phase = newPhase;
    }
}
