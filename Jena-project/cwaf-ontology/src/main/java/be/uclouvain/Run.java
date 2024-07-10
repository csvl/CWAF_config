package be.uclouvain;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import be.uclouvain.model.Directive;
import be.uclouvain.service.CompileContext;

import static be.uclouvain.service.Parser.parseConfig;
import static be.uclouvain.utils.OntUtils.readStreamFromFile;

import java.util.stream.Stream;

import static be.uclouvain.service.Compiler.compileConfig;
import static be.uclouvain.service.Compiler.printStreamDump;

public class Run {

    public static void fullProcess(String[] args) {
        OntModel model;
        try {
            model = parseConfig(args[0]);
            OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
            schema.read("Jena-project/ontCWAF_0.7.ttl", "TTL");
            
            CompileContext ctx = new CompileContext(model, schema);
    
            OntModel ontExec = ModelFactory.createOntologyModel();
    
            Stream<Directive> order = compileConfig(ctx, ontExec);
            printStreamDump(ctx, order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {

        // fullProcess(args);

        Stream<Directive> order = readStreamFromFile("global_order.ser");

        // order.filter(d -> {
        //     for (String arg : d.getArgs()) {
        //         if (arg.contains("var=")) {
        //             return true;
        //         }
        //     }
        //     return false;
        // }).forEach(d -> System.out.println(d));
        order.forEach(d -> System.out.println(d));
    }

}
