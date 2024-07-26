package be.uclouvain.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static be.uclouvain.utils.DirectiveFactory.parseArguments;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

import com.github.andrewoma.dexx.collection.Pair;

import be.uclouvain.model.Directive;
import be.uclouvain.service.DirectiveContext;
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
        if (uniqueName == null || uniqueName.length() == 0){
            throw new IllegalArgumentException("Macro name cannot be null or empty");
        }
        if (uniqueName.startsWith(OntCWAF.NS)) {
            return uniqueName;
        }
        return OntCWAF.NS + "Macro_" + uniqueName.toLowerCase();
    }

    public static String getMacroNameFromURI(String URI) {
        return URI.replace(OntCWAF.NS + "Macro_", "");
    }

    public static void cleanPlaceHolders(OntModel model) {
        List<Statement> stmts = model.listLiteralStatements(null, OntCWAF.IS_PLACE_HOLDER, true).toList();
        for (Statement stmt : stmts) {
            stmt.getSubject().as(Individual.class).remove();
        }
    }

    public static String getURIForName(String name) {
        return OntCWAF.NS + name + "_" + getUUID().toString();
    }


    public static String renewURI(String oldURI) {
        //use regex to replace the UUID after the last _ by a new one
        String regex = "^(.*_).*?$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(oldURI);
        if (matcher.matches()) {
            return matcher.group(1) + getUUID().toString();
        } else {
            System.err.println("No UUID found in " + oldURI);
            return oldURI;
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


    public static void saveOntology(String filePath, OntModel model, String format, boolean withSchema) {
        if (withSchema) {
            OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
            schema.read("Jena-project/ontCWAF_1.0.ttl", "TTL");
            model.add(schema);
        }
        saveOntology(filePath, model, format);
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

    public static void attachDirectiveToOnt(OntModel model, DirectiveContext context, Individual directive, Resource file) {
        if (directive.hasOntClass(OntCWAF.ELSE_IF) || directive.hasOntClass(OntCWAF.ELSE)) {
            if (context.lastIf == "") {
                System.err.println("Error: ELSE or ELSE_IF directive without IF, ignoring directive at line " +
                                directive.getPropertyValue(OntCWAF.DIR_LINE_NUM).asLiteral().getInt() + " in file " + file.getLocalName());
                return;
            }
            Resource lastIf = model.getResource(context.lastIf);
            lastIf.addProperty(OntCWAF.IF_CHAIN, directive);
        }
        else if (context.beaconStack.size() > 0){
           Resource beacon = model.getResource(context.beaconStack.peek()); 
           beacon.addProperty(OntCWAF.CONTAINS_DIRECTIVE, directive);
        } else {
            file.addProperty(OntCWAF.CONTAINS_DIRECTIVE, directive);
        }
    }

    public static void writeStreamToFile(String filename, Stream<Directive> strm) {
        try (FileOutputStream fileOut = new FileOutputStream(filename);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(strm.toList());
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static Stream<Directive> readStreamFromFile(String filename) {
        Stream<Directive> data = null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("global_order.ser"))) {
            data = ((List<Directive>) ois.readObject()).stream();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return data;
    }

    // public static void expandVarsInIndividual(Individual ind, String[] keys, String[] values) {
    //     List<Statement> stmts = ind.listProperties().toList();
    //     for (Statement stmt : stmts) {
    //         if (stmt.getObject().isLiteral()) {
    //             String content = stmt.getObject().asLiteral().getString();
    //             String[] parsed = parseArguments(content, null);
    //             String[] replaced = StringUtils.replaceContentInArray(keys, values, parsed);
    //             String newContent = String.join(" ", replaced);
    //             if (!newContent.equals(content)) {
    //                 ind.removeProperty(stmt.getPredicate(), stmt.getObject());
    //                 ind.addProperty(stmt.getPredicate(), newContent);
    //             }
    //         } else if (stmt.getObject().canAs(Individual.class) && !stmt.getPredicate().equals(OntCWAF.INSTANCE_OF)) {
    //             Individual object = stmt.getObject().as(Individual.class);
    //             expandVarsInIndividual(object, keys, values);
    //         }
    //     }
    // }

    public static void expandVarsInIndividual(Individual ind, String[] keys, String[] values) {
        Stack<Individual> stack = new Stack<>();
        Set<Individual> visited = new HashSet<>();
        stack.push(ind);
        visited.add(ind);

        while (!stack.isEmpty()) {
            Individual current = stack.pop();
            List<Statement> stmts = current.listProperties().toList();

            for (Statement stmt : stmts) {
                if (stmt.getObject().isLiteral()) {
                    String content = stmt.getObject().asLiteral().getString();
                    String[] parsed = parseArguments(content, null);
                    String[] replaced = StringUtils.replaceContentInArray(keys, values, parsed);
                    String newContent = String.join(" ", replaced);

                    if (!newContent.equals(content)) {
                        current.removeProperty(stmt.getPredicate(), stmt.getObject());
                        current.addProperty(stmt.getPredicate(), newContent);
                    }
                } else if (stmt.getObject().canAs(Individual.class) && !stmt.getPredicate().equals(OntCWAF.INSTANCE_OF)) {
                    Individual object = stmt.getObject().as(Individual.class);
                    if (!visited.contains(object)) {
                        stack.push(object);
                        visited.add(object);
                    }
                }
            }
        }
    }

    // public static Individual copyIndividual(Individual ind, OntModel model) {
    //     String newURI = renewURI(ind.getURI());
    //     Individual newInd = model.createIndividual(newURI, ind.getOntClass());
    //     newInd.addProperty(OntCWAF.INSTANCE_OF, ind);

    //     for (Statement stmt : ind.listProperties().toList()) {
    //         if (stmt.getObject().canAs(Individual.class)) {
    //             newInd.addProperty(stmt.getPredicate(), copyIndividual(stmt.getObject().as(Individual.class), model));
    //         } else if (stmt.getObject().isLiteral()) {
    //             Literal lit = stmt.getObject().asLiteral();
    //             if (lit.getDatatypeURI().equals(XSD.xstring.getURI())) {
    //                 newInd.addLiteral(stmt.getPredicate(), new String(lit.getString()));
    //             } else {
    //                 // newInd.addLiteral(stmt.getPredicate(), lit.getValue());
    //                 newInd.addLiteral(stmt.getPredicate(),
    //                     ResourceFactory.createTypedLiteral(lit.getLexicalForm(), lit.getDatatype()));
    //             }
    //         } else {
    //             newInd.addProperty(stmt.getPredicate(), stmt.getObject());
    //         }
    //     }
    //     return newInd;
    // }

  public static Individual copyIndividual(Individual ind, OntModel model) {
        Map<String, Individual> copiedIndividuals = new HashMap<>();
        Queue<Pair<String, Individual>> queue = new LinkedList<>();
        queue.add(new Pair<String,Individual>(renewURI(ind.getURI()), ind));

        while (!queue.isEmpty()) {
            Pair<String, Individual> currentPair = queue.poll();
            Individual current = currentPair.component2();
            String currentURI = current.getURI();

            // If already copied, skip this individual
            if (copiedIndividuals.containsKey(currentURI)) {
                continue;
            }

            // Create a new individual
            String newURI = currentPair.component1();
            System.err.println("Copying " + currentURI + " to " + newURI);
            Individual newInd = model.createIndividual(newURI, current.getRDFType());
            newInd.addProperty(OntCWAF.INSTANCE_OF, current);

            copiedIndividuals.put(currentURI, newInd);

            for (Statement stmt : current.listProperties().toList()) {
                if (stmt.getObject().canAs(Individual.class)) {
                    Individual objectInd = stmt.getObject().as(Individual.class);
                    String objectNewURI = renewURI(objectInd.getURI());
                    queue.add(new Pair<String,Individual>(objectNewURI, objectInd));
                    newInd.addProperty(stmt.getPredicate(),
                            copiedIndividuals.getOrDefault(objectInd.getURI(),
                                            model.createIndividual(objectNewURI, objectInd.getOntClass()))); 
                } else if (stmt.getObject().isLiteral()) {
                    Literal lit = stmt.getObject().asLiteral();
                    if (lit.getDatatypeURI().equals(XSD.xstring.getURI())) {
                        newInd.addLiteral(stmt.getPredicate(), new String(lit.getString()));
                    } else {
                        newInd.addLiteral(stmt.getPredicate(),
                            ResourceFactory.createTypedLiteral(lit.getLexicalForm(), lit.getDatatype()));
                    }
                } else {
                    newInd.addProperty(stmt.getPredicate(), stmt.getObject());
                }
            }
        }

        return copiedIndividuals.get(ind.getURI());
    }

}
