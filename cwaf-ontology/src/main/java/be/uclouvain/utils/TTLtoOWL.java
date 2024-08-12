package be.uclouvain.utils;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;

public class TTLtoOWL extends Object {

    public static void main (String args[]) {
    
        Model model = RDFDataMgr.loadModel(args[0]) ;
        model.write(System.out);
    }

}
