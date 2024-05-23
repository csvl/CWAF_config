package be.uclouvain;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.function.library.print;
import org.apache.jena.vocabulary.VCARD;

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
            System.out.print(subject.getLocalName());
            System.out.print(" " + predicate.getLocalName() + " ");
            if (object instanceof Resource) {
                System.out.print(object.asResource().getLocalName());
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

    public static void main (String args[]) {
    
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);

        Individual config = ontModel.createIndividual(OntCWAF.CONFIGURATION);
        config.addProperty(OntCWAF.CONFIG_NAME, "first conf");
        
        Bag file_bag = ontModel.createBag();

        Individual file1 = ontModel.createIndividual(OntCWAF.FILE);
        file1.addLiteral(OntCWAF.FILEPATH, "file1.txt");
        Individual file2 = ontModel.createIndividual(OntCWAF.FILE);
        file2.addLiteral(OntCWAF.FILEPATH, "file2.txt");

        file_bag.add(file1);
        file_bag.add(file2);

        Individual directive1 = ontModel.createIndividual(OntCWAF.RULE);
        directive1.addLiteral(OntCWAF.LINE_NUM, 1);
        directive1.addProperty(OntCWAF.HAS_SCOPE,
                ontModel.createIndividual(OntCWAF.SCOPE)
                    .addProperty(OntCWAF.HAS_LOCATION, 
                        ontModel.createIndividual(OntCWAF.LOCATION)
                            .addProperty(OntCWAF.LOCATION_PATH, "/")
                    ).addProperty(OntCWAF.HAS_SERVER,
                        ontModel.createIndividual(OntCWAF.SERVER)
                            .addLiteral(OntCWAF.LISTEN, "80")
                            .addLiteral(OntCWAF.SERVER_NAME, "localhost")
                    ));

        file1.addProperty(OntCWAF.CONTAINS_DIRECTIVE, directive1);

        config.addProperty(OntCWAF.CONTAINS_FILES, file_bag);

        // ontModel.write(System.out, "TTL");

        saveOntology("output.ttl", ontModel, "TTL");

    }

}
