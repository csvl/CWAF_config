package be.uclouvain.utils;

import static be.uclouvain.utils.OntUtils.getMacroNameFromURI;

import java.util.List;

import org.apache.jena.ontology.*;

import be.uclouvain.service.context.DirectiveContext;
import be.uclouvain.vocabulary.OntCWAF;

public class DirectiveFactory {
    
    public static String[] parseArguments(String macroArgs, Individual use) {
        if (macroArgs == null || macroArgs.length() == 0) {
            return new String[0];
        }
        String[] args = macroArgs.split(" +(?:(?=(?:[^\"\']*(?:(\"[^\"]*\")|(\'[^\']*\')))*[^\"\']*$))");
        for (int i = 0; i < args.length; i++) {
                args[i] = args[i].replaceAll("^[\"\']|[\"\']$", "");
        }
        return args;
    }

    public static Individual createUse(OntModel model, DirectiveContext context, int line_num, String args,String macroURI) {
        Individual use = createRule(model, context, line_num, "Use", args, OntCWAF.USE);

        Individual macro = model.getIndividual(macroURI);
        if (macro == null) {
            macro = createMacro(model, context, -1, getMacroNameFromURI(macroURI), "");
            macro.addLiteral(OntCWAF.IS_PLACE_HOLDER, true);
        }
        use.addProperty(OntCWAF.USE_MACRO, macro);
        return use;
    }

    public static Individual createModSecRule(OntModel model, DirectiveContext context, int line_num, String name, String args, int phase, Integer id, List<String> tags) {
       Individual modSecRuleIndividual = createRule(model, context, line_num, name, args, OntCWAF.MOD_SEC_RULE);
       modSecRuleIndividual.addLiteral(OntCWAF.PHASE, phase);
         if (id != null) {
            modSecRuleIndividual.addLiteral(OntCWAF.RULE_ID, id);
        }
        if (!tags.isEmpty()) {
            for (String tag : tags) {
                modSecRuleIndividual.addLiteral(OntCWAF.RULE_TAG, tag);
            }
        }
        return modSecRuleIndividual;
    }

    public static Individual createRemoveById(OntModel model, DirectiveContext context, int line_num, String args) {
        Individual removeById = createRule(model, context, line_num, "ModSecRemoveById", args, OntCWAF.MOD_SEC_RULE);
        return removeById;
    }

    public static Individual createRemoveByTag(OntModel model, DirectiveContext context, int line_num, String tag) {
        Individual removeByTag = createRule(model, context, line_num, "ModSecRemoveByTag", tag, OntCWAF.MOD_SEC_RULE);
        return removeByTag;
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
