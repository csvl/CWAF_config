package be.uclouvain;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import be.uclouvain.model.Directive;
import be.uclouvain.service.CompileContext;

import static be.uclouvain.service.Parser.parseConfig;

import java.util.stream.Stream;

import static be.uclouvain.service.Compiler.compileConfig;
import static be.uclouvain.service.Compiler.printStreamDump;

public class Run {

    public static void main(String[] args) {
        OntModel model;
        try {
            model = parseConfig(args[0]);
            OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
            schema.read("Jena-project/ontCWAF_1.0.ttl", "TTL");
            
            CompileContext ctx = new CompileContext(model, schema);
    
            OntModel ontExec = ModelFactory.createOntologyModel();
    
            Stream<Directive> order = compileConfig(ctx, ontExec);
            printStreamDump(ctx, order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
