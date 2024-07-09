package be.uclouvain;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import be.uclouvain.service.CompileContext;

import static be.uclouvain.service.Parser.parseConfig;
import static be.uclouvain.service.Compiler.compileConfig;

public class Run {
    
    public static void main(String[] args) {
        OntModel model;
        try {
            model = parseConfig(args[0]);
            OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
            schema.read("Jena-project/ontCWAF_0.7.ttl", "TTL");
            
            CompileContext ctx = new CompileContext(model, schema);
    
            OntModel ontExec = ModelFactory.createOntologyModel();
    
            compileConfig(ctx, ontExec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
