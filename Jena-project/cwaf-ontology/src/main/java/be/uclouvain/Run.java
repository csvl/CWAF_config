package be.uclouvain;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import be.uclouvain.model.Directive;
import be.uclouvain.service.context.CompileContext;

import static be.uclouvain.service.Parser.parseConfig;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static be.uclouvain.service.Compiler.compileConfig;
import static be.uclouvain.utils.OntUtils.*;

public class Run {

    public static void main(String[] args) {
        OntModel model;
        try {
            model = parseConfig(args[0]);

            saveOntology("config.ttl", model, "TTL");
            saveOntology("full_schema.ttl", model, "TTL", true);

            OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
            schema.read("Jena-project/ontCWAF_1.0.ttl", "TTL");

            CompileContext ctx = new CompileContext(model, schema);
            model.close();
            schema.close();
    
            OntModel ontExec = ModelFactory.createOntologyModel();
    
            Stream<Directive> global_order = compileConfig(ctx, ontExec);
            OntModel ontEntity = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
            List<Directive> orderList = global_order.collect(Collectors.toList());
            orderList.forEach(d -> {
                d.toEntityIndividual(ontEntity);
            });

            saveOntology("entities.ttl", ontEntity, "TTL");
            saveOntology("full_entities.ttl", ontEntity, "TTL", true);

            writeStreamToFile("global_order.ser", orderList.stream());
            // HttpRequest request = HttpRequest.newBuilder()
            //         .uri(URI.create("http://localhost/test/special/data"))
            //         .build();

            // Stream<Directive> filtered = filterFromRequest(order, request);
            // filtered.forEach(System.out::println);
            orderList.forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
