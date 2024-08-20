package be.uclouvain.service.context;

import org.apache.jena.ontology.Individual;

import be.uclouvain.model.Directive;
import be.uclouvain.vocabulary.OntCWAF;

public class CallTraceElem {
    
    String callingURI;
    String calledURI;
    String fileURI;
    int lineNum;

    public CallTraceElem(Directive directive, Individual called) {
        this.callingURI = directive.getIndividual().getURI();
        this.calledURI = called.getURI();
        this.fileURI = directive.getFileURI();
        this.lineNum = directive.getLineNum();
    }

    public CallTraceElem(String callingURI, String calledURI, String fileURI, int lineNum) {
        this.callingURI = callingURI;
        this.calledURI = calledURI;
        this.fileURI = fileURI;
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        String file = fileURI.replace(OntCWAF.NS, "");
        return calledURI + " from " + callingURI + "(" + file + ":" + lineNum + ")";
    }
}
