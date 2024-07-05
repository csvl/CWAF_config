package be.uclouvain.service;

import org.apache.jena.rdf.model.*;

import be.uclouvain.model.Condition;
import be.uclouvain.model.Directive;
import be.uclouvain.vocabulary.OntCWAF;

import static be.uclouvain.service.Constants.Parser.phasePattern;
import static be.uclouvain.utils.DirectiveFactory.parseArguments;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;

public class Compiler {

    public static void printStreamDump(OntModel ontFS, Stream<Directive> dump) {
        dump.forEach( dir -> {
            System.out.print(dir);
            StmtIterator stmts = ontFS.listStatements(null, OntCWAF.CONTAINS_DIRECTIVE, dir.getIndividual());
            // stmts.forEach(System.out::println);
            stmts.forEach( stmt -> {
                if (stmt.getSubject().as(Individual.class).hasProperty(OntCWAF.FILE_PATH)) {
                    System.out.print(" in " + stmt.getSubject().as(Individual.class).getPropertyValue(OntCWAF.FILE_PATH).asLiteral().getString() + ":" + dir.getLineNum());
                }
            });
            System.out.println(".");
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

        // Seq args = use.getIndividual().getProperty(OntCWAF.USE_PARAMS).getSeq(); //TODO change property
        String[] args = use.getArgs();
        System.err.println( use.getIndividual().getLocalName() + " args:" + Arrays.toString(args));
        return compileMacro(ontFS, ctx, macro, args, use.getScope());
    }

    private static Stream<Directive> compileMacro(OntModel ontFS, CompileContext ctx, Individual macro, String[] args, String[] use_scope) {
        String paramsStr = macro.getPropertyValue(OntCWAF.MACRO_ARGS).asLiteral().getString();
        String[] params = parseArguments(paramsStr, null);
        if (params.length != args.length) {
            throw new InvalidParameterException("Number of arguments Mismatch for macro " + macro.getLocalName()
            + ": Expected " + params.length + ", got " + args.length + "\n (" + Arrays.toString(params) + " vs " + Arrays.toString(args) + ")");
            
        }
        for (int i = 0; i < params.length; i++) {
            ctx.addVar(params[i], args[i], macro.getURI());
        }
        Stream<Directive> res = compileContainer(ontFS, ctx, macro).map(d -> {
            d.setScope(use_scope);
            return d;
        });
        ctx.removeTaggedVar(macro.getURI());
        return res;
    }

    private static Stream<Directive> compileIf(OntModel ontFS, CompileContext ctx, Individual ifInd) {
        String conditionStr = ifInd.getPropertyValue(OntCWAF.CONDITION).asLiteral().getString();
        Condition condition = new Condition(conditionStr);
        ctx.addEC(condition);
        Stream<Directive> res = compileContainer(ontFS, ctx, ifInd);
        ctx.popEC();
        if (ifInd.hasProperty(OntCWAF.IF_CHAIN)) {
            Individual ifBlock = ifInd.getProperty(OntCWAF.IF_CHAIN).getObject().as(Individual.class);
            ctx.addEC(condition.not());
            if (ifBlock.hasProperty(OntCWAF.CONDITION)) { //= is ELSE_IF //TODO faute du graph ?
                res = Stream.concat(res, compileIf(ontFS, ctx, ifBlock));
            } else {
                res = Stream.concat(res, compileContainer(ontFS, ctx, ifBlock));
            }
            ctx.popEC();
        }
        return res;
    }

    private static String[] replaceContentInArgs(CompileContext ctx, String[] content){
        for (int i = 0; i < content.length; i++) {
            content[i] = replaceContent(ctx, content[i]);
        }
        return content;
    }

    private static String replaceContent(CompileContext ctx, String content){
        for (String var : ctx.getLocalVars().keySet()) {
            String regex = Matcher.quoteReplacement(var);
            String localVal = Matcher.quoteReplacement(ctx.getLocalVars().get(var));
            content = content.replaceAll(regex, localVal);
        }
        return content;
    }

    private static void expandVars(OntModel ontFS, CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasOntClass(OntCWAF.RULE)) {
            String[] content = replaceContentInArgs(ctx, directive.getArgs());
            directive.setArgs(content);
            updateDirective(ontFS, ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.BEACON)) {
            if (directiveInd.hasOntClass(OntCWAF.IF) || directiveInd.hasOntClass(OntCWAF.ELSE_IF)) {
                if (directiveInd.hasProperty(OntCWAF.CONDITION)) {
                    String condition = directiveInd.getPropertyValue(OntCWAF.CONDITION).asLiteral().getString();
                    condition = replaceContent(ctx, condition);
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
            String[] args = directive.getArgs();
            for (String arg : args) {
                Matcher phaseMatcher = phasePattern.matcher(arg);
                if (phaseMatcher.find()) {
                    int phase = Integer.parseInt(phaseMatcher.group(1));
                    directiveInd.addLiteral(OntCWAF.PHASE, phase);
                    directive.setPhase(phase);
                    break;
                }
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
        } else if (directiveInd.hasOntClass(OntCWAF.BEACON)) {
            if (directiveInd.hasOntClass(OntCWAF.IF)) {
                return compileIf(ontFS, ctx, directive.getIndividual());
            } else if (directiveInd.hasOntClass(OntCWAF.MACRO)) {
                return Stream.empty();
            } else {
                return compileContainer(ontFS, ctx, directiveInd);
            }
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
