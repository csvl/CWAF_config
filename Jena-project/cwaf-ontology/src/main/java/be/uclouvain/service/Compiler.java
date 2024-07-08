package be.uclouvain.service;

import org.apache.jena.rdf.model.*;

import be.uclouvain.model.Condition;
import be.uclouvain.model.Directive;
import be.uclouvain.vocabulary.OntCWAF;

import static be.uclouvain.service.Constants.Parser.phasePattern;
import static be.uclouvain.utils.DirectiveFactory.parseArguments;

import java.security.InvalidParameterException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;

public class Compiler {

    public static void printStreamDump(CompileContext ctx, Stream<Directive> dump) {
        OntModel ontFS = ctx.getInfModel();
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
    
    public static void compileConfig(CompileContext ctx, OntModel ontExec) {
        Individual config = ctx.getModel().getIndividual(OntCWAF.NS + "config");
        Bag file_bag = config.getProperty(OntCWAF.CONTAINS_FILE).getObject().as(Bag.class);
        Individual first_file = file_bag.iterator().next().as(Individual.class);
        Stream<Directive> dump_like = compileFile(ctx, first_file).sorted();
        // dump_like.forEach(a -> {});
        printStreamDump(ctx, dump_like);
    }

    private static Stream<Directive> getOrderedDirectives(CompileContext ctx, Individual container) {
        OntModel m = ctx.getModel();
        List<Directive> directives = new ArrayList<>();
        m.listStatements(container, OntCWAF.CONTAINS_DIRECTIVE, (RDFNode) null).forEach(stmt -> {
            String URI = stmt.getObject().as(Individual.class).getURI();
            directives.add(new Directive(ctx, ctx.getModel().getIndividual(URI)));
        });
        // container.listProperties(OntCWAF.CONTAINS_DIRECTIVE).forEach(stmt -> {
        //     Individual directive = stmt.getObject().as(Individual.class);
        //     directives.add(new Directive(ctx, directive));
        // });
        // System.out.println("Directives in " + container.getLocalName() + ": " + directives.size());
        // for (Directive directive : directives) {
        //     System.out.println("\t- " + directive);
        // }
        return directives.stream().sorted((d1, d2) -> Integer.compare(d1.getLineNum(), d2.getLineNum()));
    }

    public static Stream<Directive> compileContainer(CompileContext ctx, Individual container) {
        ctx.push(container);
        Stream<Directive> directives = getOrderedDirectives(ctx, container);
        CompileContext ctx_copy = new CompileContext(ctx);
        Stream<Directive> res = directives.flatMap( dir -> compileDirective(ctx_copy, dir));
        ctx.pop();
        return res;
    }

    public static Stream<Directive> compileFile(CompileContext ctx, Individual file) {
        return compileContainer(ctx, file);
    }

    public static Stream<Directive> compileFile(CompileContext ctx, Individual file, String[] scope) {
        if (scope != null) {
            return compileContainer(ctx, file).map(d -> {
                // d.setScope(scope);
                System.err.println("Scope set to " + Arrays.toString(scope) + " for directive " + d.getIndividual().getLocalName());
                return d;
            });
        }
        return compileContainer(ctx, file);
    }

    private static Stream<Directive> expandInclude(CompileContext ctx, Directive include) {
        Individual file = include.getIndividual().getProperty(OntCWAF.INCLUDE_FILE).getObject().as(Individual.class);
        return compileFile(ctx, file);//, include.getScope()
    }

    private static Stream<Directive> expandUse(CompileContext ctx, Directive use) {
        String macroURI = use.getIndividual().getProperty(OntCWAF.USE_MACRO).getObject().asLiteral().getString();
        if (!ctx.isMacroDefined(macroURI)) {
            System.err.println("Macro not defined: " + macroURI);
            System.err.println("Used in directive: " + use.getIndividual().getURI());
            return Stream.empty();
        }
        Individual macro = ctx.getModel().getIndividual(macroURI);
        if (macro == null) {
            System.err.println("Macro defined but not found: " + macroURI);
            System.err.println("Used in directive: " + use.getIndividual().getURI());
            return Stream.empty();
        }
        String[] args = use.getArgs();
        // System.err.println("Using macro: " + macro.getLocalName() + " with args " + Arrays.toString(args));
        return compileMacroContent(ctx, macro, args); //, use.getScope()
    }

    private static Stream<Directive> compileMacroContent(CompileContext ctx, Individual macro, String[] args) { //, String[] use_scope
        String paramsStr = macro.getPropertyValue(OntCWAF.MACRO_PARAMS).asLiteral().getString();
        String[] params = parseArguments(paramsStr, null);
        if (params.length != args.length) {
            throw new InvalidParameterException("Number of arguments Mismatch for macro " + macro.getLocalName()
            + ": Expected " + params.length + ", got " + args.length + "\n (" + Arrays.toString(params) + " vs " + Arrays.toString(args) + ")");
            
        }
        for (int i = 0; i < params.length; i++) {
            ctx.addVar(params[i], args[i], macro.getURI());
        }
        Stream<Directive> res = compileContainer(ctx, macro);
        // .map(d -> {
        //     d.setScope(use_scope);
        //     System.err.println("Directive " + d.getIndividual().getLocalName() + " in " + macro.getLocalName() + " with scope " + Arrays.toString(use_scope));
        //     return d;
        // });
        ctx.removeTaggedVar(macro.getURI());
        return res;
    }

    private static Stream<Directive> compileIf(CompileContext ctx, Individual ifInd) {
        String conditionStr = ifInd.getPropertyValue(OntCWAF.CONDITION).asLiteral().getString();
        Condition condition = new Condition(conditionStr);
        ctx.addEC(condition);
        Stream<Directive> res = compileContainer(ctx, ifInd);
        ctx.popEC();
        if (ifInd.hasProperty(OntCWAF.IF_CHAIN)) {
            Individual ifBlock = ifInd.getProperty(OntCWAF.IF_CHAIN).getObject().as(Individual.class);
            ctx.addEC(condition.not());
            if (ifBlock.hasProperty(OntCWAF.CONDITION)) { //= is ELSE_IF //TODO faute du graph ?
                res = Stream.concat(res, compileIf(ctx, ifBlock));
            } else {
                res = Stream.concat(res, compileContainer(ctx, ifBlock));
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

    private static void replaceProperty(CompileContext ctx, Individual ind, DatatypeProperty prop){
        String content = ind.getPropertyValue(prop).asLiteral().getString();
        content = replaceContent(ctx, content);
        ind.removeAll(prop);
        ind.addLiteral(prop, content);
    }

    private static void expandVars(CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasOntClass(OntCWAF.RULE)) {
            if (directiveInd.hasOntClass(OntCWAF.VIRTUAL_HOST)) {
                replaceProperty(ctx, directiveInd, OntCWAF.VIRTUAL_HOST_NAME);
            } else if (directiveInd.hasOntClass(OntCWAF.LOCATION)) {
                replaceProperty(ctx, directiveInd, OntCWAF.LOCATION_PATH);
            } else {
                String[] content = replaceContentInArgs(ctx, directive.getArgs());
                directive.setArgs(content);
                updateDirective(ctx, directive);
            }
        } else if (directiveInd.hasOntClass(OntCWAF.BEACON)) {
            if (directiveInd.hasOntClass(OntCWAF.IF) || directiveInd.hasOntClass(OntCWAF.ELSE_IF)) {
                if (directiveInd.hasProperty(OntCWAF.CONDITION)) {
                    replaceProperty(ctx, directiveInd, OntCWAF.CONDITION);
                } else {
                    System.err.println("Condition not found for If " + directiveInd.getLocalName());
                }
            }
        } else {
            System.err.println("Directive " + directiveInd.getLocalName() + " is not a rule or beacon");
        }
    }

    private static void updateDirective(CompileContext ctx, Directive directive) {
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

    private static Stream<Directive> compileScopeRule(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasOntClass(OntCWAF.VIRTUAL_HOST)) {
            String vhost = directiveInd.getPropertyValue(OntCWAF.VIRTUAL_HOST_NAME).asLiteral().getString();
            ctx.setCurrentVirtualHost(vhost);
        } else if (directiveInd.hasOntClass(OntCWAF.LOCATION)) {
            String loc = directiveInd.getPropertyValue(OntCWAF.LOCATION_PATH).asLiteral().getString();
            ctx.setCurrentLocation(loc);
        } else if (directiveInd.hasOntClass(OntCWAF.END_VIRTUAL_HOST)) {
            ctx.resetCurrentVirtualHost();
        } else if (directiveInd.hasOntClass(OntCWAF.END_LOCATION)) {
            ctx.resetCurrentLocation();
        } else {
            System.err.println("Unknown Scope Rule: " + directiveInd.getLocalName());
        }
        return Stream.empty();
    }

    private static Stream<Directive> compileDefine(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        String args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(args, null);
        if (content.length == 1) {
            ctx.addVar("${"+content[0]+"}", null);
        } else if (content.length == 2) {
            ctx.addVar("${"+content[0]+"}", content[1]);
        } else {
            System.err.println("Invalid number of arguments for Define: " + content.length);
        }
        return Stream.empty();
    }

    private static Stream<Directive> compileUndefine(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        String args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(args, null);
        if (content.length == 1) {
            ctx.removeVar("${"+content[0]+"}");
        } else {
            System.err.println("Invalid number of arguments for Undefine: " + content.length);
        }
        return Stream.empty();
    }

    private static Stream<Directive> compileUndefMacro(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        String macroURI = directiveInd.getURI();
        ctx.undefineMacro(macroURI);
        return Stream.empty();
    }

    private static Stream<Directive> compileGenericRule(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasProperty(OntCWAF.RULE_TYPE)) {
            String ruleType = directiveInd.getPropertyValue(OntCWAF.RULE_TYPE).asLiteral().getString().toLowerCase();
            switch (ruleType) {
                case "define":
                    return compileDefine(ctx, directive);
                case "undefmacro":
                    return compileUndefMacro(ctx, directive);
                case "undefine":
                    return compileUndefine(ctx, directive);
                default:
                    return Stream.of(directive);
            }
        } else {
            System.err.println("Rule Type not found for directive " + directiveInd.getLocalName());
        }
        return Stream.empty();
    }

    private static Stream<Directive> compileDefineMacro(CompileContext ctx, Individual directiveInd){
        String macroURI = directiveInd.getURI();
        ctx.defineMacro(macroURI);
        return Stream.empty();
    }

    private static Stream<Directive> compileDirective(CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        expandVars(ctx, directive);
        if (directiveInd.hasOntClass(OntCWAF.BEACON)) {
            if (directiveInd.hasOntClass(OntCWAF.IF)) {
                return compileIf(ctx, directive.getIndividual());
            } else if (directiveInd.hasOntClass(OntCWAF.MACRO)) {
                return compileDefineMacro(ctx, directiveInd);
            } else {
                return compileContainer(ctx, directiveInd);
            }
        } else if (directiveInd.hasOntClass(OntCWAF.INCLUDE)) {
            return expandInclude(ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.USE)) {
            return expandUse(ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.SCOPE_RULE)) {
            return compileScopeRule(ctx, directive);
        } else if (directiveInd.hasProperty(OntCWAF.RULE_TYPE)) {
            return compileGenericRule(ctx, directive);
        } else {
            return Stream.of(directive);
        }
    }

    public static void main(String[] args) {
        OntModel ontFS = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        ontFS.read("config.ttl", "TTL");

        OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        schema.read("Jena-project/ontCWAF_0.6.ttl", "TTL");
        // ontFS.add(schema);
        // ontFS.write(System.out, "TTL");
        // System.exit(0);
        CompileContext ctx = new CompileContext(ontFS, schema);

        OntModel ontExec = ModelFactory.createOntologyModel();

        compileConfig(ctx, ontExec);

    }
}
