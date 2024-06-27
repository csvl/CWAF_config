package be.uclouvain.utils;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Seq;

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

    private static Seq parseArguments(String macroArgs, Individual use) {
        String[] args = macroArgs.split(" +(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        Seq argsInd = use.getModel().createSeq(use.getURI()+"_argsSeq"); //TODO change URI
        for (String arg : args) {
            argsInd.add(arg);
        }
        return argsInd;
    }

    public static Individual createUse(OntModel model, DirectiveContext context, int line_num, String args,String macroURI) {
        Individual use = createRule(model, context, line_num, "Use", "", OntCWAF.USE);
        Seq argsInd = parseArguments(args, use);
        use.addProperty(OntCWAF.USE_PARAMS, argsInd);
        use.addLiteral(OntCWAF.USE_MACRO, macroURI);
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
        return directiveInd;
    }

    public static Individual createDirective(OntModel model, DirectiveContext context, int line_num, String name) {
        return createDirective(model, context, line_num, name, OntCWAF.DIRECTIVE);
    }

    private static void initDirective(OntModel model, DirectiveContext context, int line_num, String URI, Individual directiveInd) {
        directiveInd.addLiteral(OntCWAF.DIR_LINE_NUM, line_num);
        if (context.currentLocation != "" || context.currentVirtualhost != "" || context.serverName != "") {
            Individual scope = model.createIndividual(URI+"-scope", OntCWAF.SCOPE);
            if (context.currentLocation != "") {
                scope.addProperty(OntCWAF.HAS_LOCATION, model.createIndividual(URI+"-scope-location", OntCWAF.LOCATION)
                        .addLiteral(OntCWAF.LOCATION_PATH, context.currentLocation));
            }
            if (context.currentVirtualhost != "") {
                scope.addProperty(OntCWAF.HAS_VIRTUAL_HOST,
                    model.createIndividual(URI+"-scope-vhost", OntCWAF.VIRTUAL_HOST)
                    .addLiteral(OntCWAF.V_HOST_NAME, context.currentVirtualhost));
            }
            if (context.serverName != "") {
                scope.addProperty(OntCWAF.HAS_SERVER, model.createIndividual(URI+"-scope-server", OntCWAF.SERVER)
                .addLiteral(OntCWAF.SERVER_NAME, context.serverName)
                .addLiteral(OntCWAF.SERVER_PORT, context.serverPort));
            }
            directiveInd.addProperty(OntCWAF.HAS_SCOPE, scope);
        }
    }


    public static Individual createRule(OntModel model, DirectiveContext context, int line_num, String name, String args) {
        return createRule(model, context, line_num, name, args, OntCWAF.RULE);
    }

    public static Individual createRule(OntModel model, DirectiveContext context, int line_num, String name, String args, OntClass type) {
        Individual ruleInd = createDirective(model, context, line_num, name, type);
        // ruleInd.addLiteral(OntCWAF.RULE_NAME, name); //TODO ruleName or dirName ?
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

    public static Individual createMacro(OntModel model, DirectiveContext context, int line_num, String name, String paramsString) {
        String URI = OntUtils.getMacroURI(name);
        Individual macro = model.createIndividual(URI, OntCWAF.MACRO);
        initDirective(model, context, line_num, URI, macro);
        macro.addLiteral(OntCWAF.MACRO_NAME, name);
        Seq params = parseArguments(paramsString, macro);
        macro.addProperty(OntCWAF.MACRO_ARGS, params);
        return macro;
    }

    public static Individual createIf(OntModel model, DirectiveContext context, int line_num, String condition) {
        Individual ifInd = createBeacon(model, context, line_num, "If", OntCWAF.IF);
        ifInd.addLiteral(OntCWAF.CONDITION, condition);
        return ifInd;
    }

    public static Individual createElseIf(OntModel model, DirectiveContext context, int line_num, String condition) {
        Individual ifInd = createBeacon(model, context, line_num, "ElseIf", OntCWAF.ELSE_IF);
        ifInd.addLiteral(OntCWAF.CONDITION, condition);
        return ifInd;
    }

    public static Individual createElse(OntModel model, DirectiveContext context, int line_num) {
        return createBeacon(model, context, line_num, "Else", OntCWAF.ELSE);
    }

}
