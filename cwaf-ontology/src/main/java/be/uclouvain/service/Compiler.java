package be.uclouvain.service;

import org.apache.jena.rdf.model.*;

import be.uclouvain.model.Condition;
import be.uclouvain.model.Directive;
import be.uclouvain.model.LocalVar;
import be.uclouvain.service.context.CompileContext;
import be.uclouvain.vocabulary.OntCWAF;

import static be.uclouvain.service.Constants.Parser.*;
import static be.uclouvain.utils.DirectiveFactory.parseArguments;
import static be.uclouvain.utils.OntUtils.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;

public class Compiler {
    
    public static List<Directive> compileConfig(CompileContext ctx, OntModel ontExec) {
        defineStrPass(ctx);
        Individual config = ctx.getModel().getIndividual(OntCWAF.NS + "config");
        Seq file_bag = config.getProperty(OntCWAF.CONTAINS_FILE).getObject().as(Seq.class);
        Individual first_file = file_bag.getResource(1).as(Individual.class);
        List<Directive> global_order = compileFile(ctx, first_file).stream().sorted().toList();
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

    private static List<Directive> getOrderedDirectives(CompileContext ctx, Individual container) {
        OntModel m = ctx.getModel();
        List<Directive> directives = new ArrayList<>();
        StmtIterator tmp = m.listStatements(container, OntCWAF.DIRECT_CONTAINS_DIRECTIVE, (RDFNode) null);
        tmp.forEach(stmt -> {
            String URI = stmt.getObject().as(Individual.class).getURI();
            directives.add(new Directive(ctx, ctx.getModel().getIndividual(URI)));
        });
        return directives.stream().sorted((d1, d2) -> Integer.compare(d1.getLineNum(), d2.getLineNum())).collect(Collectors.toList());
    }

    public static List<Directive> compileContainer(CompileContext ctx, Individual container) {
        ctx.stackTracePush(container);
        List<Directive> directives = getOrderedDirectives(ctx, container);
        List<Directive> res = new ArrayList<>();
        for (Directive directive : directives) {
            res.addAll(compileDirective(ctx, directive));
        }
        ctx.stackTracePop();
        return res;
    }

    public static List<Directive> compileFile(CompileContext ctx, Individual file) {
        return compileContainer(ctx, file);
    }


    private static List<Directive> expandInclude(CompileContext ctx, Directive include) {
        Individual file = include.getIndividual().getProperty(OntCWAF.INCLUDE_FILE).getObject().as(Individual.class);
        ctx.callTracePush(include, file);
        List<Directive> res = compileFile(ctx, file);
        ctx.callTracePop();
        return res;//, include.getScope()
    }

    private static List<Directive> expandUse(CompileContext ctx, Directive use) {
        if (!use.getIndividual().hasProperty(OntCWAF.USE_MACRO)) {
            System.err.println("Macro not found in Use directive: " + use.getIndividual().getLocalName());
            return new ArrayList<>();
        }
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

        ctx.callTracePush(use, macro);
        List<Directive> res = compileMacroContent(ctx, macro, args);
        ctx.callTracePop();
        return res; //, use.getScope()
    }

    private static List<Directive> compileMacroContent(CompileContext ctx, Individual macro, String[] args) { //, String[] use_scope
        String paramsStr = macro.getPropertyValue(OntCWAF.MACRO_PARAMS).asLiteral().getString();
        String[] params = parseArguments(paramsStr, null);
        if (params.length != args.length) {
            System.err.println("Number of arguments Mismatch for macro " + macro.getLocalName()
            + ": Expected " + params.length + ", got " + args.length + "\n (" + Arrays.toString(params) + " vs " + Arrays.toString(args) + ")");
            return new ArrayList<>();
        }
        for (int i = 0; i < params.length; i++) {
            ctx.addVar(params[i], args[i], macro.getURI());
        }
        List<Directive> res = compileContainer(ctx, macro);
        ctx.removeTaggedVar(macro.getURI());
        return res;
    }

    private static List<Directive> compileIf(CompileContext ctx, Individual ifInd) {
        String conditionStr = ifInd.getPropertyValue(OntCWAF.CONDITION).asLiteral().getString();
        Condition condition = new Condition(conditionStr);
        ctx.addEC(condition);
        List<Directive> res = compileContainer(ctx, ifInd);
        ctx.popEC();
        if (ifInd.hasProperty(OntCWAF.IF_CHAIN)) {
            Individual ifBlock = ifInd.getProperty(OntCWAF.IF_CHAIN).getObject().as(Individual.class);
            ctx.addEC(condition.not());
            if (ifBlock.hasProperty(OntCWAF.CONDITION)) { 
                res = Stream.concat(res.stream(), compileIf(ctx, ifBlock).stream()).collect(Collectors.toList());
            } else {
                res = Stream.concat(res.stream(), compileContainer(ctx, ifBlock).stream()).collect(Collectors.toList());
                res = Stream.concat(res.stream(), compileContainer(ctx, ifBlock).stream()).collect(Collectors.toList());
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
            System.err.println("Directive " + directiveInd.getLocalName() + " is not a rule nor beacon");
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
                }
                Matcher idMatcher = idPattern.matcher(arg);
                if (idMatcher.find()) {
                    Integer id = Integer.parseInt(idMatcher.group(1));
                    directiveInd.addLiteral(OntCWAF.RULE_ID, id);
                    directive.setId(id);
                }
                Matcher tagMatcher = tagPattern.matcher(arg);
                while (tagMatcher.find()) {
                    String tag = tagMatcher.group(1);
                    tag = tag.replaceAll("^[\"\']|[\"\']$", "");
                    directive.addTag(tag);
                }
            }
        }
    }

    private static List<Directive> compileScopeRule(CompileContext ctx, Directive directive){
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
            return new ArrayList<>();
    }

    private static List<Directive> compileDefine(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        String args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(args, null);
        if (content.length != 0 && ctx.getLocalVar("${"+content[0]+"}") != null) { //TODO create "contains var"
            return new ArrayList<>();
        }
        if (content.length == 1) {
            ctx.addVar("${"+content[0]+"}", null);
        } else if (content.length == 2) {
            ctx.addVar("${"+content[0]+"}", content[1]);
        } else {
            System.err.println("Invalid number of arguments for Define: " + content.length);
        }
        return List.of(directive);
    }

    private static List<Directive> compileDefineStr(CompileContext ctx, Individual directiveInd){
        String args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(args, null);
        if (content.length == 2) {
            ctx.addVar("~{"+content[0]+"}", content[1], "DefineStr");
        } else {
            System.err.println("Invalid number of arguments for DefineStr: " + content.length + " in directive " + directiveInd.getLocalName());
        }
        return new ArrayList<>();
    }

    private static List<Directive> compileUndefine(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        String args = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(args, null);
        if (content.length == 1) {
            ctx.removeVar("${"+content[0]+"}");
        } else {
            System.err.println("Invalid number of arguments for Undefine: " + content.length);
        }
        return new ArrayList<>();
    }

    private static List<Directive> compileUndefMacro(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        String arg = directiveInd.getPropertyValue(OntCWAF.ARGUMENTS).asLiteral().getString();
        String[] content = parseArguments(arg, null);
        if (content.length >= 1) {
            String macroURI = getMacroURI(content[0]);
            ctx.undefineMacro(macroURI);
        } else {
            System.err.println("Invalid number of arguments for UndefMacro: " + content.length);
        }
        return new ArrayList<>();
    }

    private static List<Directive> applyRemoveById(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        String[] content = directive.getArgs();
        for (String args : content) {
            for (String arg : args.split("\\s*,\\s*")) {
                if (arg.contains("-")) {
                    String[] range = arg.split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    for (int i = start; i <= end; i++) {
                        Directive.removeById(i, directiveInd.getURI());
                    }
                } else {
                    int id = Integer.parseInt(arg);
                    Directive.removeById(id, directiveInd.getURI());
                }
            }
        }
        return List.of(directive);
    }

    private static List<Directive> applyRemoveByTag(CompileContext ctx, Directive directive) {
        String[] args = directive.getArgs();
        if (args.length != 1) {
            System.err.println("Invalid number of arguments for RemoveByTag: " + args);
            return List.of(directive);
        }
        Directive.removeByTag(args[0], directive.getIndividual().getURI());
        return List.of(directive);
    }

    private static List<Directive> compileGenericRule(CompileContext ctx, Directive directive){
        Individual directiveInd = directive.getIndividual();
        if (directiveInd.hasProperty(OntCWAF.DIR_TYPE)) {
            String ruleType = directiveInd.getPropertyValue(OntCWAF.DIR_TYPE).asLiteral().getString().toLowerCase();
            switch (ruleType) {
                case "define":
                    return compileDefine(ctx, directive);
                case "definestr":
                    return new ArrayList<>();
                case "undefmacro":
                    return compileUndefMacro(ctx, directive);
                case "undefine":
                    return compileUndefine(ctx, directive);
                case "modsecremovebyid":
                    return applyRemoveById(ctx, directive);
                case "modsecremovebytag":
                    return applyRemoveByTag(ctx, directive);
                case "secdefaultaction":
                    return List.of(directive); //TODO
                default:
                    return List.of(directive);
            }
        } else {
            System.err.println("Rule Type not found for directive " + directiveInd.getLocalName());
        }
        return new ArrayList<>();
    }

    private static List<Directive> compileDefineMacro(CompileContext ctx, Individual directiveInd){
        String macroURI = directiveInd.getURI();
        ctx.defineMacro(macroURI);
        return new ArrayList<>();
    }

    private static List<Directive> compileGenericIf(CompileContext ctx, Directive directive){
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
                        return new ArrayList<>();
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
                        return new ArrayList<>();
                    }
                    break;
                default:
                    break;
            }
            if (condition == null) {
                return compileContainer(ctx, ifInd);
            }
            ctx.addEC(condition);
            List<Directive> res = compileContainer(ctx, ifInd);
            ctx.popEC();
            return res;
        } else {
            System.err.println("If Type not found for directive " + ifInd.getLocalName());
        }
        return compileContainer(ctx, ifInd);
    }

    private static List<Directive> compileDirective(CompileContext ctx, Directive directive) {
        Individual directiveInd = directive.getIndividual();
        directive.updateContext(ctx);
        expandVars(ctx, directive);
        if (directiveInd.hasOntClass(OntCWAF.BEACON)) {
            if (directiveInd.hasOntClass(OntCWAF.IF)) {
                return compileIf(ctx, directive.getIndividual());
            } else if (directiveInd.hasOntClass(OntCWAF.MACRO)) {
                return compileDefineMacro(ctx, directiveInd);
            } else if (directiveInd.hasProperty(OntCWAF.IF_TYPE)) {
                return compileGenericIf(ctx, directive);
            // } else if (directiveInd.hasProperty(OntCWAF.DIR_TYPE)) {
            //     return compileGenericContainer(ctx, directive);
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
            return List.of(directive);
        }
    }

    public static void main(String[] args) {
        OntModel ontFS = ModelFactory.createOntologyModel();
        ontFS.read("config.ttl", "TTL");

        OntModel schema = ModelFactory.createOntologyModel();
        schema.read("ontCWAF_1.1.ttl", "TTL");
        
        CompileContext ctx = new CompileContext(ontFS, schema);

        OntModel ontExec = ModelFactory.createOntologyModel();

        List<Directive> global_order = compileConfig(ctx, ontExec);

        OntModel ontEntity = ModelFactory.createOntologyModel();
        global_order.forEach(d -> {
            d.toEntityIndividual(ontEntity);
        });
        saveOntology("entities.ttl", ontEntity, "TTL");
        saveOntology("full_entities.ttl", ontEntity, "TTL", true);

        // printStreamDump(ctx, global_order);
        writeStreamToFile("global_order.ser", global_order.stream());
    }
}
