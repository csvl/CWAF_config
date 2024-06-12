package be.uclouvain.utils;

import java.util.Iterator;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

public class OntUtils {
    
    public static void print_bag(Bag bag) {
        for (Iterator<RDFNode> i = bag.iterator(); i.hasNext();) {
            RDFNode file = i.next();
            System.out.println(file.asResource().getLocalName()); 
        }
    }

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
                System.out.print(object.asResource().getLocalName());
            } else {
                // object is a literal
                System.out.print(" \"" + object.asLiteral().toString() + "\"");
            }

            System.out.println(" .");
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

}
