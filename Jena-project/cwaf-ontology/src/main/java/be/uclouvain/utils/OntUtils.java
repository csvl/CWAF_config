package be.uclouvain.utils;

import java.util.*;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

import be.uclouvain.service.Context;
import be.uclouvain.vocabulary.OntCWAF;

public class OntUtils {
    
    static final String UUID_SEED = "0n70l0gI3S R 7H3 PHU7UR3";
    static final Random random = new Random(UUID_SEED.hashCode());

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

    public static UUID getUUID() {
        byte[] b = new byte[16];
        random.nextBytes(b);
        return UUID.nameUUIDFromBytes(b);
    }

    public static String getMacroURI(String uniqueName) {
        return OntCWAF.NS + "Macro_" + uniqueName;
    }

    public static String getURIForName(String name) {
        return OntCWAF.NS + name + "_" + getUUID().toString();
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

    public static Resource findFileContaining(OntModel model, Individual directive) {
        ResIterator iter = model.listResourcesWithProperty(OntCWAF.CONTAINS_DIRECTIVE, directive);
        if (iter.hasNext()) {
            return iter.next();
        } else {
            return null;
        }
    }

    public static List<Individual> listDirectivesInFile(OntModel model, Resource file) {
        List<Individual> directives = new ArrayList<>();
        NodeIterator iter = model.listObjectsOfProperty(file, OntCWAF.CONTAINS_DIRECTIVE);
        while (iter.hasNext()) {
            directives.add(model.getIndividual(iter.next().asResource().getURI()));
        }
        return directives;
    }

    public static void attachDirectiveToOnt(OntModel model, Context context, Individual directive, Resource file) {
        if (context.beaconStack.size() > 0){
           Resource beacon = model.getResource(context.beaconStack.peek()); 
           beacon.addProperty(OntCWAF.EMBED_DIRECTIVE, directive);
        } else {
            file.addProperty(OntCWAF.CONTAINS_DIRECTIVE, directive);
        }
    }

}
