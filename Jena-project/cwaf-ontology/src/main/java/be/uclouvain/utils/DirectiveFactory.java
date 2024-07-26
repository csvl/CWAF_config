package be.uclouvain.utils;

import static be.uclouvain.utils.OntUtils.*;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;

import be.uclouvain.service.DirectiveContext;
import be.uclouvain.vocabulary.OntCWAF;

public class DirectiveFactory {
    
    public static Individual createGeneralDirective(OntModel model, DirectiveContext context, String name, int line_num) {
        if (context.beaconStack.size() > 0) {
            // return createMacro(model, context);
            //TODO
            return null;
        } else {
            return createDirective(model, context, line_num, name, OntCWAF.RULE);
        }
    }

    public static String[] parseArguments(String macroArgs, Individual use) {
        if (macroArgs == null || macroArgs.length() == 0) {
            return new String[0];
        }
        String[] args = macroArgs.split(" +(?:(?=(?:[^\"\']*(?:(\"[^\"]*\")|(\'[^\']*\')))*[^\"\']*$))");
        for (int i = 0; i < args.length; i++) {
            // if (!args[i].contains(" ")) {
                args[i] = args[i].replaceAll("^[\"\']|[\"\']$", "");
                // if (args[i].contains(" ") || args[i].length() == 0) {
                //     args[i] = "'" + args[i] + "'";
                // }
            // }
        }
        // Seq argsInd = use.getModel().createSeq(use.getURI()+"_argsSeq"); //TODO change URI
        // for (String arg : args) {
        //     argsInd.add(arg);
        // }
        // return argsInd;
        return args;
    }

    public static void populateClonedContainer(OntModel model, Individual container, Individual clonedContainer, String[] params, String[] args) {
        if (args.length != params.length) {
            System.err.println("Error: number of arguments does not match number of parameters for call " + clonedContainer.getLocalName());
            return;
        }
         List<Statement> stmts = model.listStatements(container, OntCWAF.CONTAINS_DIRECTIVE, (RDFNode)null).toList();
        System.err.println("stmts: " + stmts);
         for (Statement stmt : stmts) {
            Individual original = stmt.getObject().as(Individual.class);
            Individual instance = copyIndividual(original, model);
            instance.setOntClass(OntCWAF.DIR_INSTANCE);
            expandVarsInIndividual(instance, params, args);
            clonedContainer.addProperty(OntCWAF.CONTAINS_INSTANCE, instance);
        }
    }


    public static void populateMacroCall(OntModel model, Individual call, Individual macro, String useArgs) {
        String[] args = parseArguments(useArgs, null);
        String[] params = parseArguments(macro.getPropertyValue(OntCWAF.MACRO_PARAMS).asLiteral().getString(), null);
        // System.err.println("args: " + Arrays.toString(args));
        // System.err.println("params: " + Arrays.toString(params));
        populateClonedContainer(model, macro, call, params, args);
    }

    public static Individual createMacroCall(OntModel model, Individual macro, String useArgs) {
        String name = getMacroNameFromURI(macro.getURI());
        Individual call = model.createIndividual(getURIForName("Call_" + name), OntCWAF.MACRO_CALL);
        call.addLiteral(OntCWAF.DIR_TYPE, "MacroCall"); //FIXME need of that ?
        call.addProperty(OntCWAF.CALL_OF, macro);
        call.addLiteral(OntCWAF.SOLVED_PARAMS, useArgs);
        // populateMacroCall(model, call, macro, useArgs);
        return call;
    }

    public static Individual createUse(OntModel model, DirectiveContext context, int line_num, String args,String macroURI) {
        Individual use = createRule(model, context, line_num, "Use", args, OntCWAF.USE);
        // Seq argsInd = parseArguments(args, use);
        // use.addProperty(OntCWAF.USE_PARAMS, argsInd);
        Individual macro = model.getIndividual(macroURI);
        if (macro == null) {
            macro = createMacro(model, context, -1, getMacroNameFromURI(macroURI), "");
            macro.addLiteral(OntCWAF.IS_PLACE_HOLDER, true);
            // System.err.println("Error: macro " + getMacroNameFromURI(macroURI) + " not found" + "; with URI: " + macroURI);
            // return null;
        }
        Individual call = createMacroCall(model, macro, args);
        use.addProperty(OntCWAF.CALL, call);
        return use;
    }

    public static Individual createModSecRule(OntModel model, DirectiveContext context, int line_num, String name, String args, int phase) {
       Individual modSecRuleIndividual = createRule(model, context, line_num, name, args, OntCWAF.MOD_SEC_RULE);
       modSecRuleIndividual.addLiteral(OntCWAF.PHASE, phase);
        return modSecRuleIndividual;
    }

    public static Individual createInclude(OntModel model, DirectiveContext context, String name, int line_num, Individual parsedFile) {
        Individual include = createRule(model, context, line_num, name, "", OntCWAF.INCLUDE);
        include.addProperty(OntCWAF.INCLUDE_FILE, parsedFile);
        return include;
    }

    public static Individual createDirective(OntModel model, DirectiveContext context, int line_num, String name, OntClass type) {
        String URI = OntUtils.getURIForName(name);
        Individual directiveInd = model.createIndividual(URI, type);
        initDirective(model, context, line_num, URI, directiveInd);
        directiveInd.addLiteral(OntCWAF.DIR_TYPE, name);
        return directiveInd;
    }

    public static Individual createDirective(OntModel model, DirectiveContext context, int line_num, String name) {
        return createDirective(model, context, line_num, name, OntCWAF.DIRECTIVE);
    }

    private static void initDirective(OntModel model, DirectiveContext context, int line_num, String URI, Individual directiveInd) {
        directiveInd.addLiteral(OntCWAF.DIR_LINE_NUM, line_num);
    }


    public static Individual createRule(OntModel model, DirectiveContext context, int line_num, String name, String args) {
        return createRule(model, context, line_num, name, args, OntCWAF.RULE);
    }

    public static Individual createRule(OntModel model, DirectiveContext context, int line_num, String name, String args, OntClass type) {
        Individual ruleInd = createDirective(model, context, line_num, name, type);
        if (name != null && name != "")
            ruleInd.addLiteral(OntCWAF.DIR_TYPE, name);
        if (args != null && args != "")
            ruleInd.addLiteral(OntCWAF.ARGUMENTS, args);
        return ruleInd;
    }

    public static Individual createBeacon(OntModel model, DirectiveContext context, int line_num, String name, OntClass type) {
        return createDirective(model, context, line_num, name, type);
    }

    public static Individual createBeacon(OntModel model, DirectiveContext context, int line_num, String name) {
        return createDirective(model, context, line_num, name, OntCWAF.BEACON);
    }

    public static Individual createBeacon(OntModel model, DirectiveContext context, int line_num, String name, String args) {
        Individual dir = createDirective(model, context, line_num, name, OntCWAF.BEACON);
        dir.addLiteral(OntCWAF.ARGUMENTS, args);
        return dir;
    }

    public static Individual createMacro(OntModel model, DirectiveContext context, int line_num, String name, String paramsString) {
        String URI = OntUtils.getMacroURI(name);
        Individual macro = model.getIndividual(URI);
        if (macro != null) {
            macro.removeAll(null);
        } else {
            macro = model.createIndividual(URI, OntCWAF.MACRO);
        }
        initDirective(model, context, line_num, URI, macro);
        macro.addLiteral(OntCWAF.DIR_TYPE, "Macro");
        macro.addLiteral(OntCWAF.MACRO_NAME, name);
        macro.addLiteral(OntCWAF.MACRO_PARAMS, paramsString);
        return macro;
    }

    public static Individual createIfFamily(OntModel model, DirectiveContext context, int line_num, String name, String condition) {
        return createIfFamily(model, context, line_num, name, condition, OntCWAF.IF_FAMILY);
    }

    public static Individual createIfFamily(OntModel model, DirectiveContext context, int line_num, String name, String condition, OntClass type) {
        Individual ifInd = createBeacon(model, context, line_num, name, type);
        ifInd.addLiteral(OntCWAF.CONDITION, condition);
        return ifInd;
    }

    public static Individual createIf(OntModel model, DirectiveContext context, int line_num, String condition) {
        return createIfFamily(model, context, line_num, "If", condition, OntCWAF.IF);
    }

    public static Individual createIfRule(OntModel model, DirectiveContext context, int line_num, String rule, String arg) {
        Individual ifInd = createIfFamily(model, context, line_num, "IfRule", arg, OntCWAF.IF_RULE);
        ifInd.addLiteral(OntCWAF.IF_TYPE, rule);
        return ifInd;
    }

    public static Individual createElseIf(OntModel model, DirectiveContext context, int line_num, String condition) {
        return createIfFamily(model, context, line_num, "ElseIf", condition, OntCWAF.ELSE_IF);
    }

    public static Individual createElse(OntModel model, DirectiveContext context, int line_num) {
        return createBeacon(model, context, line_num, "Else", OntCWAF.ELSE);
    }

    public static Individual createVirtualHost(OntModel model, DirectiveContext context, int line_num, String name) {
        Individual vhost = createDirective(model, context, line_num, "VirtualHost", OntCWAF.VIRTUAL_HOST);
        vhost.addLiteral(OntCWAF.VIRTUAL_HOST_NAME, name);
        return vhost;
    }

    public static Individual createEndVirtualHost(OntModel model, DirectiveContext context, int line_num, String vhost_URI) {
        Individual vhost = createDirective(model, context, line_num, "EndVirtualHost", OntCWAF.END_VIRTUAL_HOST);
        vhost.addProperty(OntCWAF.IS_ENDING_VIRTUAL_HOST, model.getIndividual(vhost_URI));
        return vhost;
    }

    public static Individual createLocation(OntModel model, DirectiveContext context, int line_num, String path) {
        Individual loc = createDirective(model, context, line_num, "Location", OntCWAF.LOCATION);
        loc.addLiteral(OntCWAF.LOCATION_PATH, path);
        return loc;
    }

    public static Individual createEndLocation(OntModel model, DirectiveContext context, int line_num, String loc_URI) {
        Individual eloc = createDirective(model, context, line_num, "EndLocation", OntCWAF.END_LOCATION);
        eloc.addProperty(OntCWAF.IS_ENDING_LOCATION, model.getIndividual(loc_URI));
        return eloc;
    }

}
