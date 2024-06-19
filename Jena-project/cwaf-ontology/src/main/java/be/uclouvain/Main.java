package be.uclouvain;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import be.uclouvain.vocabulary.OntCWAF;

public class Main extends Object {

    public static void print_all_statements(Model model) {
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt      = iter.nextStatement();  // get next statement
            Resource  subject   = stmt.getSubject();     // get the subject
            Property  predicate = stmt.getPredicate();   // get the predicate
            RDFNode   object    = stmt.getObject();      // get the object

            // System.out.print(stmt.toString() + " ");
            System.out.print(subject.toString());
            System.out.print(" " + predicate.getLocalName() + " ");
            if (object instanceof Resource) {
                System.out.print(object.asResource().toString());
            } else {
                // object is a literal
                System.out.print(" \"" + object.asLiteral().toString() + "\"");
            }

            System.out.println(" .");
        }
    }


    public static void saveOntology(String filePath, OntModel model) {
        // Save the ontology to a file
        try (OutputStream out = new FileOutputStream(filePath)) {
            model.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveOntology(String filePath, OntModel model, String format) {
        // Save the ontology to a file
        try (OutputStream out = new FileOutputStream(filePath)) {
            model.write(out, format);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printStatements(Model m, Resource s, Property p, Resource o) {
    for (StmtIterator i = m.listStatements(s,p,o); i.hasNext(); ) {
        Statement stmt = i.nextStatement();
        System.out.println(" - " + PrintUtil.print(stmt));
    }
}

    public static void main (String args[]) {
    
        // OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);

        // Individual config = ontModel.createIndividual(OntCWAF.CONFIGURATION);
        // config.addProperty(OntCWAF.CONFIG_NAME, "first conf");
        
        // Bag file_bag = ontModel.createBag();

        // Individual file1 = ontModel.createIndividual(OntCWAF.FILE);
        // file1.addLiteral(OntCWAF.FILEPATH, "file1.txt");
        // Individual file2 = ontModel.createIndividual(OntCWAF.FILE);
        // file2.addLiteral(OntCWAF.FILEPATH, "file2.txt");

        // file_bag.add(file1);
        // file_bag.add(file2);

        // Individual directive1 = ontModel.createIndividual(OntCWAF.RULE);
        // directive1.addLiteral(OntCWAF.LINE_NUM, 1);
        // directive1.addProperty(OntCWAF.HAS_SCOPE,
        //         ontModel.createIndividual(OntCWAF.SCOPE)
        //             .addProperty(OntCWAF.HAS_LOCATION, 
        //                 ontModel.createIndividual(OntCWAF.LOCATION)
        //                     .addProperty(OntCWAF.LOCATION_PATH, "/")
        //             ).addProperty(OntCWAF.HAS_SERVER,
        //                 ontModel.createIndividual(OntCWAF.SERVER)
        //                     .addLiteral(OntCWAF.LISTEN, "80")
        //                     .addLiteral(OntCWAF.SERVER_NAME, "localhost")
        //             ));

        // file1.addProperty(OntCWAF.CONTAINS_DIRECTIVE, directive1);

        // config.addProperty(OntCWAF.CONTAINS_FILES, file_bag);

        // // ontModel.write(System.out, "TTL");

        // saveOntology("output.ttl", ontModel, "TTL");

        // ========================================
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);

        model.read("config_save.ttl", "TTL");
        // // print_all_statements(model);
        // Individual directive = model.getIndividual("file:///home/wiauxb/Documents/CWAF/CWAF_config/AddType_b57da7c4-709c-4553-967e-f8d6308858a2");
        // System.out.println(directive);
        // Resource file = OntUtils.findFileContaining(model, directive);
        // System.out.println(file == null ? "Not found" : file.getLocalName());
        // List<Individual> directives = OntUtils.listDirectivesInFile(model, file);
        // for (Individual d : directives) {
        //     System.out.println(d.getLocalName() + "at line " + d.getPropertyValue(OntCWAF.DIR_LINE_NUM).asLiteral().getInt());
        // }
        // ========================================

        //init a reasonner
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        // reasoner.getReasonerCapabilities().write(System.out);

        //load and bind the schema model
        OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        schema.read("Jena-project/ontCWAF_0.4.ttl", "TTL");

        //bind the schema to the reasoner
        // reasoner.bind(model.getGraph());
        reasoner.bindSchema(schema);

        //merge model and schema
        model.add(schema);

        Resource test = model.getResource("http://visualdataweb.org/ontCWAF/Use_15162356-5b4e-3a8a-84c0-01d45cfdfd75");
        // model.listStatements(null, null, test, null).forEach( stmt -> {
        //     System.out.println(stmt.getSubject().getLocalName());
        // });
        model.listStatements(null, null, test).forEach( stmt -> {
            if (!stmt.getPredicate().equals(OWL.differentFrom) && !stmt.getPredicate().equals(OWL.sameAs)) {
                System.out.println(stmt.getSubject().getLocalName() + " - " + stmt.getPredicate().getLocalName());
            }
        });

        model.listStatements(test, OntCWAF.USE_MACRO, (RDFNode)null).forEach( stmt -> {
            System.out.println(stmt.getObject().asLiteral().getString());
        });

        //create an inference model
        // InfModel inf = ModelFactory.createInfModel(reasoner, model);
        // InfModel inf = ModelFactory.createRDFSModel(schema, model);

        // Resource test = inf.getResource("http://visualdataweb.org/ontCWAF/SecAction_36f3be18-4060-3312-ac92-9e58b6e2696a");
        // System.out.println("test has types:");
        // printStatements(inf, test, RDF.type, null);

        // Resource use = inf.getResource("http://visualdataweb.org/ontCWAF/Use_15162356-5b4e-3a8a-84c0-01d45cfdfd75");
        // System.out.println("use contained in:");
        // printStatements(inf, null, OntCWAF.CONTAINS_DIRECTIVE, use);

        // Resource file = inf.getResource("http://visualdataweb.org/ontCWAF/ModSecEnv/httpd_rules/common/macros/aaa-misc.conf");
        // System.out.println("http.conf contains:");
        // printStatements(inf, file, OntCWAF.CONTAINS_DIRECTIVE, null);

        //query the inference model
        // String queryString = """
        //                     PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        //                     PREFIX owl: <http://www.w3.org/2002/07/owl#>
        //                     PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        //                     PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        //                     PREFIX cwaf: <http://visualdataweb.org/ontCWAF/>
        //                     SELECT ?Resource 
        //                     WHERE {
        //                         ?Resource cwaf:containsDirective cwaf:Use_15162356-5b4e-3a8a-84c0-01d45cfdfd75
        //                     }""";
        //                         // ?class rdfs:subClassOf* cwaf:Directive .
        //                     //?directive rdf:type ?class .
        //                     // FILTER ( ?file = cwaf:ModSecEnv/httpd_rules/common/macros/aaa-misc.conf )
                                
        // Query query = QueryFactory.create(queryString);
        // QueryExecution qexec = QueryExecutionFactory.create(query, inf);
        // ResultSet results = qexec.execSelect();
        
        // // //print query results
        // ResultSetFormatter.out(System.out, results, query);

        // // //close the query execution
        // qexec.close();

    }

}
