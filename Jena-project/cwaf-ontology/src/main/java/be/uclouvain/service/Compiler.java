package be.uclouvain.service;

import org.apache.jena.rdf.model.*;

import be.uclouvain.model.Directive;
import be.uclouvain.vocabulary.OntCWAF;

import static be.uclouvain.service.Constants.Parser.phasePattern;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;

public class Compiler {

    public static void printStreamDump(Model ontFS, Stream<Directive> dump) {
        dump.forEach( dir -> {
            System.out.print(dir);
            StmtIterator stmts = ontFS.listStatements(null, OntCWAF.CONTAINS_DIRECTIVE, dir.getIndividual());
            // stmts.forEach(System.out::println);
            stmts.forEach( stmt -> {
                if (stmt.getSubject().as(Individual.class).hasOntClass(OntCWAF.FILE)) {
                    try {
                        System.out.println(" in " + stmt.getSubject().as(Individual.class).getPropertyValue(OntCWAF.FILE_PATH).asLiteral().getString() + ":" + dir.getLineNum());
                    } catch (Exception e) {
                        System.out.println(";");
                    }
                    // System.out.println(" in " + stmt.getSubject().as(Individual.class).getPropertyValue(OntCWAF.FILE_PATH).asLiteral().getString() + ":" + dir.getLineNum());
                } else {
                    System.out.println(":");
                }
            });
        });
    }
    
    public static void compileConfig(OntModel ontFS, OntModel ontExec) {
        Individual config = ontFS.getIndividual(OntCWAF.NS + "config");
        Bag file_bag = config.getProperty(OntCWAF.CONTAINS_FILE).getObject().as(Bag.class);
        Individual first_file = file_bag.iterator().next().as(Individual.class);
        CompileContext ctx = new CompileContext();
        Stream<Directive> dump_like = compileFile(ontFS, ctx, first_file).sorted();
        // dump_like.forEach(a -> {});
        printStreamDump(ontFS, dump_like);
    }

    private static Stream<Directive> getOrderedDirectives(OntModel ontFS, CompileContext ctx, Individual container) {
        List<Directive> directives = new ArrayList<>();
        container.listProperties(OntCWAF.CONTAINS_DIRECTIVE).forEach(stmt -> {
            Individual directive = stmt.getObject().as(Individual.class);
            directives.add(new Directive(ctx, directive));
        });
        return directives.stream();
    }

    public static Stream<Directive> compileContainer(OntModel ontFS, CompileContext ctx, Individual container) {
        ctx.push(container);
        Stream<Directive> directives = getOrderedDirectives(ontFS, ctx, container);
        CompileContext ctx_copy = new CompileContext(ctx);
        Stream<Directive> res = directives.flatMap( dir -> compileDirective(ontFS, ctx_copy, dir));
        ctx.pop();
        return res;
    }

    public static Stream<Directive> compileFile(OntModel ontFS, CompileContext ctx, Individual file) {
        return compileContainer(ontFS, ctx, file);
    }

    private static Stream<Directive> expandInclude(OntModel ontFS, CompileContext ctx, Directive include) {
        Individual file = include.getIndividual().getProperty(OntCWAF.INCLUDE_FILE).getObject().as(Individual.class);
        return compileFile(ontFS, ctx, file);
    }

    private static Stream<Directive> expandUse(OntModel ontFS, CompileContext ctx, Directive use) {
        String macroURI = use.getIndividual().getProperty(OntCWAF.USE_MACRO).getObject().asLiteral().getString();
        Individual macro = ontFS.getIndividual(macroURI);
        if (macro == null) {
            System.err.println("Macro not found: " + macroURI);
            System.err.println("Used in directive: " + use.getIndividual().getURI());
            return Stream.empty();
        }

        Seq args = use.getIndividual().getProperty(OntCWAF.USE_PARAMS).getSeq(); //TODO change property
        return compileMacro(ontFS, ctx, macro, args, use.getScope());
    }

    private static Stream<Directive> compileMacro(OntModel ontFS, CompileContext ctx, Individual macro, Seq args, String[] use_scope) {
        NodeIterator paramsIt = macro.getProperty(OntCWAF.MACRO_ARGS).getSeq().iterator();
        for (NodeIterator argsIt = args.iterator(); argsIt.hasNext();) {
            if (!paramsIt.hasNext()) {
                throw new InvalidParameterException("Number of arguments Mismatch for macro " + macro.getLocalName());
            }
            String param = paramsIt.next().toString();
            String arg = argsIt.next().toString();
            ctx.addVar(param, arg, macro.getURI());
        }
        Stream<Directive> res = compileContainer(ontFS, ctx, macro).map(d -> {
            d.setScope(use_scope);
            return d;
        });
        ctx.removeTaggedVar(macro.getURI());
        return res;
    }

    private static boolean evaluateCondition(OntModel ontFS, CompileContext ctx, Individual ifInd) {
        //TODO
        return true;
    }

    private static Stream<Directive> evaluateIf(OntModel ontFS, CompileContext ctx, Individual ifInd) {
        if (evaluateCondition(ontFS, ctx, ifInd)) {
            return compileContainer(ontFS, ctx, ifInd);
        } else if (ifInd.hasProperty(OntCWAF.IF_CHAIN)) {
                Individual ifBlock = ifInd.getProperty(OntCWAF.IF_CHAIN).getObject().as(Individual.class);
                if (ifBlock.hasOntClass(OntCWAF.ELSE_IF)) {
                    return evaluateIf(ontFS, ctx, ifBlock);
                } else {
                    return compileContainer(ontFS, ctx, ifBlock);
                }
        } else {
            return Stream.empty();
        }
    }

    private static void expandVars(OntModel ontFS, CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasOntClass(OntCWAF.RULE)) {
            RDFNode args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS);
            if (args == null) {
                return;
            }
            String content = args.asLiteral().getString();
            for (String var : ctx.getLocalVars().keySet()) {
                String regex = Matcher.quoteReplacement(var);
                String localVal = Matcher.quoteReplacement(ctx.getLocalVars().get(var));
                content = content.replaceAll(regex, localVal);
            }
            directiveInd.removeAll(OntCWAF.ARGUMENTS);
            directiveInd.addLiteral(OntCWAF.ARGUMENTS, content);
            updateDirective(ontFS, ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.BEACON)) {
            if (directiveInd.hasOntClass(OntCWAF.IF) || directiveInd.hasOntClass(OntCWAF.ELSE_IF)) {
                if (directiveInd.hasProperty(OntCWAF.CONDITION)) {
                    String condition = directiveInd.getPropertyValue(OntCWAF.CONDITION).asLiteral().getString();
                    for (String var : ctx.getLocalVars().keySet()) {
                        condition = condition.replaceAll(Pattern.quote(var), ctx.getLocalVars().get(var));
                    }
                    directiveInd.removeAll(OntCWAF.CONDITION);
                    directiveInd.addLiteral(OntCWAF.CONDITION, condition);
                } else {
                    System.err.println("Condition not found for If " + directiveInd.getLocalName());
                }
            }
        } else {
            System.err.println("Directive " + directiveInd.getLocalName() + " is not a rule or beacon");
        }
    }

    private static void updateDirective(OntModel ontFS, CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasOntClass(OntCWAF.MOD_SEC_RULE)) {
            String args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();

            Matcher phaseMatcher = phasePattern.matcher(args);
            if (phaseMatcher.find()) {
                int phase = Integer.parseInt(phaseMatcher.group(1));
                directiveInd.addLiteral(OntCWAF.PHASE, phase);
                directive.setPhase(phase);
            }
        }
    }

    private static Stream<Directive> compileDirective(OntModel ontFS, CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        expandVars(ontFS, ctx, directive);

        if (directiveInd.hasOntClass(OntCWAF.INCLUDE)) {
            return expandInclude(ontFS, ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.USE)) {
            return expandUse(ontFS, ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.IF)) {
            return evaluateIf(ontFS, ctx, directive.getIndividual());
        } else if (directiveInd.hasOntClass(OntCWAF.MACRO)) {
            return Stream.empty();
        } else {
            return Stream.of(directive);
        }
    }

    public static void main(String[] args) {
        OntModel ontFS = ModelFactory.createOntologyModel();
        ontFS.read("config.ttl", "TTL");

        OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        schema.read("Jena-project/ontCWAF_0.5.ttl", "TTL");
        ontFS.add(schema);

        OntModel ontExec = ModelFactory.createOntologyModel();

        compileConfig(ontFS, ontExec);

    }
}
