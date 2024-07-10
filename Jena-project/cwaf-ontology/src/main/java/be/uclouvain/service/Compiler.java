package be.uclouvain.service;

import org.apache.jena.rdf.model.*;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.uclouvain.model.Condition;
import be.uclouvain.model.Directive;
import be.uclouvain.model.LocalVar;
import be.uclouvain.vocabulary.OntCWAF;

import static be.uclouvain.service.Constants.Parser.phasePattern;
import static be.uclouvain.utils.DirectiveFactory.parseArguments;
import static be.uclouvain.utils.OntUtils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
            StmtIterator stmts = ontFS.listStatements(dir.getIndividual(), OntCWAF.CONTAINED_IN, (RDFNode)null);
            // stmts.forEach(System.out::println);
            stmts.forEach( stmt -> {
                if (stmt.getSubject().as(Individual.class).hasProperty(OntCWAF.FILE_PATH)) {
                    System.out.print(" in " + stmt.getSubject().as(Individual.class).getPropertyValue(OntCWAF.FILE_PATH).asLiteral().getString() + ":" + dir.getLineNum());
                }
            });
            System.out.println(".");
        });
    }
    
    public static Stream<Directive> compileConfig(CompileContext ctx, OntModel ontExec) {
        defineStrPass(ctx);
        Individual config = ctx.getModel().getIndividual(OntCWAF.NS + "config");
        Bag file_bag = config.getProperty(OntCWAF.CONTAINS_FILE).getObject().as(Bag.class);
        Individual first_file = file_bag.iterator().next().as(Individual.class);
        Stream<Directive> global_order = compileFile(ctx, first_file).sorted();
        return global_order;
    }

    public static void defineStrPass(CompileContext ctx) {
        //FIXME: this is uggly as shit, but it works and I don't have the energy to think a better solution
        Iterator<Individual> definestr = ctx.getModel().listIndividuals().filterKeep(ind -> {
            if (ind.hasProperty(OntCWAF.DIR_TYPE)) {
                String dirType = ind.getPropertyValue(OntCWAF.DIR_TYPE).asLiteral().getString().toLowerCase();
                return dirType.equals("definestr");
            }
            return false;
        });
        while (definestr.hasNext()) {
            Individual directiveInd = definestr.next();
            compileDefineStr(ctx, directiveInd);
        }
        solveAllVars(ctx);
    }

    private static void solveAllVars(CompileContext ctx) {
        Map<Boolean, List<LocalVar>> partitioned = ctx.getLocalVars().stream().collect(Collectors.partitioningBy(var -> var.value.contains("~{")));
        int to_solve = partitioned.get(true).size();
        int last_to_solve = -1;
        while (to_solve != last_to_solve && to_solve > 0) {
            List<LocalVar> vars = partitioned.get(false);
            partitioned.get(true).forEach(var -> {
                for (LocalVar var2 : vars) {
                    if (var.value.contains(var2.name)) {
                        String regex2 = Pattern.quote(var2.name);
                        String value2 = var2.value;
                        var.value = var.value.replaceAll(regex2, Matcher.quoteReplacement(value2));
                    }
                }
            });
            partitioned = ctx.getLocalVars().stream().collect(Collectors.partitioningBy(var -> var.value.contains("~{")));
            last_to_solve = to_solve; 
            to_solve = partitioned.get(true).size();
        }
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
        Individual macro = use.getIndividual().getProperty(OntCWAF.USE_MACRO).getObject().as(Individual.class);
        // if (!ctx.isMacroDefined(macroURI)) {
        //     System.err.println("Macro not defined: " + macroURI);
        //     System.err.println("Used in directive: " + use.getIndividual().getURI());
        //     return Stream.empty();
        // }
        // if (macro == null) {
        //     System.err.println("Macro defined but not found: " + macroURI);
        //     System.err.println("Used in directive: " + use.getIndividual().getURI());
        //     return Stream.empty();
        // }
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
        for (LocalVar var : ctx.getLocalVars()) {
            String regex = Pattern.quote(var.name);
            String localVal = var.value;
            if (localVal != null) {
                content = content.replaceAll(regex, Matcher.quoteReplacement(localVal));
            }
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
        if (content.length != 0 && ctx.getLocalVar("${"+content[0]+"}") != null) { //TODO create "contains var"
            return Stream.empty();
        }
        if (content.length == 1) {
            ctx.addVar("${"+content[0]+"}", null);
        } else if (content.length == 2) {
            ctx.addVar("${"+content[0]+"}", content[1]);
        } else {
            System.err.println("Invalid number of arguments for Define: " + content.length);
        }
        return Stream.empty();
    }

    private static Stream<Directive> compileDefineStr(CompileContext ctx, Individual directiveInd){
        String args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(args, null);
        if (content.length == 2) {
            ctx.addVar("~{"+content[0]+"}", content[1]);
        } else {
            System.err.println("Invalid number of arguments for DefineStr: " + content.length + " in directive " + directiveInd.getLocalName());
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
        String arg = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(arg, null);
        if (content.length >= 1) {
            String macroURI = getMacroURI(content[0]);
            ctx.undefineMacro(macroURI);
        } else {
            System.err.println("Invalid number of arguments for UndefMacro: " + content.length);
        }
        return Stream.empty();
    }

    private static Stream<Directive> compileGenericRule(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasProperty(OntCWAF.DIR_TYPE)) {
            String ruleType = directiveInd.getPropertyValue(OntCWAF.DIR_TYPE).asLiteral().getString().toLowerCase();
            switch (ruleType) {
                case "define":
                    return compileDefine(ctx, directive);
                case "definestr":
                    return Stream.empty();
                //     return compileDefineStr(ctx, directive);
                case "undefmacro":
                    return compileUndefMacro(ctx, directive);
                case "undefine":
                    return compileUndefine(ctx, directive);
                case "secdefaultaction":
                    return Stream.of(directive); //TODO
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

    private static Stream<Directive> compileGenericIf(CompileContext ctx, Directive directive){
        Individual ifInd = directive.getIndividual();
        if (ifInd.hasProperty(OntCWAF.IF_TYPE)) {
            String dirType = ifInd.getPropertyValue(OntCWAF.IF_TYPE).asLiteral().getString().toLowerCase();
            Condition condition = null;
            String cond = ifInd.getPropertyValue(OntCWAF.CONDITION).asLiteral().getString();
            String[] content = parseArguments(cond, null);
            switch (dirType) {
                case "define":
                    if (content.length >= 1) {
                        if (content[0].startsWith("!")) {
                            condition = new Condition("∃ " + content[0].substring(1)).not();
                        } else {
                            condition = new Condition("∃ " + content[0]);
                        }
                    } else {
                        System.err.println("Invalid number of arguments for IfDefine: " + content.length);
                        return Stream.empty();
                    }
                        break;
                case "module":
                    if (content.length >= 1) {
                        if (content[0].startsWith("!")) {
                            condition = new Condition("≜[" + content[0].substring(1) + "]").not();
                        } else {
                            condition = new Condition("≜[" + content[0] + "]");
                        }
                    } else {
                        System.err.println("Invalid number of arguments for IfModule: " + content.length);
                        return Stream.empty();
                    }
                    break;
                default:
                    break;
            }
            if (condition == null) {
                return compileContainer(ctx, ifInd);
            }
            ctx.addEC(condition);
            Stream<Directive> res = compileContainer(ctx, ifInd);
            ctx.popEC();
            return res;
        } else {
            System.err.println("Directive Type not found for directive " + ifInd.getLocalName());
        }
        return compileContainer(ctx, ifInd);
    }

    private static Stream<Directive> compileDirective(CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        expandVars(ctx, directive);
        if (directiveInd.hasOntClass(OntCWAF.BEACON)) {
            if (directiveInd.hasOntClass(OntCWAF.IF)) {
                return compileIf(ctx, directive.getIndividual());
            } else if (directiveInd.hasOntClass(OntCWAF.MACRO)) {
                return compileDefineMacro(ctx, directiveInd);
            } else if (directiveInd.hasProperty(OntCWAF.IF_TYPE)) {
                return compileGenericIf(ctx, directive);
            } else {
                return compileContainer(ctx, directiveInd);
            }
        } else if (directiveInd.hasOntClass(OntCWAF.INCLUDE)) {
            return expandInclude(ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.USE)) {
            return expandUse(ctx, directive);
        } else if (directiveInd.hasOntClass(OntCWAF.SCOPE_RULE)) {
            return compileScopeRule(ctx, directive);
        } else if (directiveInd.hasProperty(OntCWAF.DIR_TYPE)) {
            return compileGenericRule(ctx, directive);
        } else {
            return Stream.of(directive);
        }
    }

    public static void main(String[] args) {
        OntModel ontFS = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        ontFS.read("config.ttl", "TTL");

        OntModel schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        schema.read("Jena-project/ontCWAF_0.7.ttl", "TTL");
        // ontFS.add(schema);
        // ontFS.write(System.out, "TTL");
        // System.exit(0);
        CompileContext ctx = new CompileContext(ontFS, schema);

        OntModel ontExec = ModelFactory.createOntologyModel();

        Stream<Directive> global_order = compileConfig(ctx, ontExec);

        printStreamDump(ctx, global_order);
        // writeStreamToFile("global_order.ser", global_order);
    }
}
