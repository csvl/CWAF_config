package be.uclouvain.service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;

import be.uclouvain.utils.Directive;
import be.uclouvain.utils.OntUtils;

import be.uclouvain.vocabulary.OntCWAF;
import static be.uclouvain.utils.OntUtils.*;
import static be.uclouvain.utils.DirectiveFactory.*;
import static be.uclouvain.service.Constants.Parser.*;

public class Parser {

    // static String pwd = null;

    public static OntModel parseConfig(String filePath) throws IOException {

        OntModel confModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        
        Individual config = confModel.createIndividual(OntCWAF.NS + "config", OntCWAF.CONFIGURATION);
        config.addProperty(OntCWAF.CONFIG_NAME, "first conf");
        
        Bag file_bag = confModel.createBag(OntCWAF.NS + Constants.FILE_BAG_NAME);
        config.addProperty(OntCWAF.CONTAINS_FILE, file_bag);

        parseConfigFile(filePath, confModel, file_bag);
        cleanPlaceHolders(confModel);

        return confModel;
    }

    private static Individual parseConfigFile(String filePath, OntModel model, Bag file_bag) throws IOException{
        //TODO handle "\" for multiline directives

        // System.out.println("Including file: " + filePath);

        //Check if file is already in the bag
        //TODO optimize ?
        for (Iterator<RDFNode> i = file_bag.iterator(); i.hasNext();) {
            RDFNode file = i.next();
            if (file.asResource().getURI().equals(filePath)) {
                System.err.println( filePath + " already in the bag.");
                return model.getIndividual(file.asResource().getURI());
            }
        }

        List<String> lines = Files.readAllLines(Paths.get(filePath));

        Individual file = model.createIndividual(OntCWAF.NS + filePath, OntCWAF.FILE);
        file.addLiteral(OntCWAF.FILE_PATH, filePath);
        file_bag.add(file);

        DirectiveContext context = new DirectiveContext();

        for (int line_num = 1; line_num < lines.size()+1; line_num++){
            String line = lines.get(line_num-1);

            if (commentPattern.matcher(line).find()) {
                continue;
            }

            Matcher beaconMatcher = beaconPattern.matcher(line);
            if (beaconMatcher.find()) {
                parseBeacon(model, context, line, line_num, file);
                continue;
            } else {
                parseDirective(model, context, line, line_num, file);
            }

        }

        return file;
    }

    public static void parseBeacon(OntModel model, DirectiveContext context, String line, int line_num, Individual file) {

        Matcher virtualHostMatcher = virtualHostPattern.matcher(line);
        if (virtualHostMatcher.find()) {
            context.lastIf = "";
            String virtualHost = virtualHostMatcher.group(1);
            Individual vh = createVirtualHost(model, context, line_num, virtualHost);
            context.currentVirtualhost = vh.getURI();
            attachDirectiveToOnt(model, context, vh, file);
            return;
        }

        Matcher virtualHostEndMatcher = virtualHostEndPattern.matcher(line);
        if (virtualHostEndMatcher.find()) {
            context.lastIf = "";
            Individual evh = createEndVirtualHost(model, context, line_num, context.currentVirtualhost);
            context.currentVirtualhost = "";
            attachDirectiveToOnt(model, context, evh, file);
            return;
        }

        Matcher macroMatcher = macroPattern.matcher(line);
        if (macroMatcher.find()) {
            context.lastIf = "";
            String macroName = macroMatcher.group(1);
            String macroParams = macroMatcher.group(2);
            Individual macro = createMacro(model, context, line_num, macroName, macroParams == null ? "" : macroParams);
            attachDirectiveToOnt(model, context, macro, file);
            context.beaconStack.push(macro.getURI());
            return;
        }

        Matcher macroEndMatcher = macroEndPattern.matcher(line);
        if (macroEndMatcher.find()) {
            try {
                context.beaconStack.pop();
            } catch (EmptyStackException e) {
                System.err.println("Error: ending a macro without opening it.");
            }
            return;
        }

        Matcher ifMatcher = ifPattern.matcher(line);
        if (ifMatcher.find()) {
            context.lastIf = "";
            String condition = ifMatcher.group(1);
            Individual ifInd = createIf(model, context, line_num, condition);
            attachDirectiveToOnt(model, context, ifInd, file);
            context.beaconStack.push(ifInd.getURI());
            return;
        }

        Matcher ifEndMatcher = ifEndPattern.matcher(line);
        if (ifEndMatcher.find()) {
            context.lastIf = context.beaconStack.pop();
            return;
        }

        Matcher ifRuleMatcher = ifRulePattern.matcher(line);
        if (ifRuleMatcher.find()) {
            String rule = ifRuleMatcher.group(1);
            String args = ifRuleMatcher.group(2);
            Individual ifInd = createIfRule(model, context, line_num, rule, args);
            attachDirectiveToOnt(model, context, ifInd, file);
            context.beaconStack.push(ifInd.getURI());
            return;
        }

        Matcher ifRuleEndMatcher = ifRuleEndPattern.matcher(line);
        if (ifRuleEndMatcher.find()) {
            context.lastIf = context.beaconStack.pop();
            return;
        }

        Matcher elseIfMatcher = elseIfPattern.matcher(line);
        if (elseIfMatcher.find()) {
            String condition = elseIfMatcher.group(1);
            Individual elseIfInd = createElseIf(model, context, line_num, condition);
            attachDirectiveToOnt(model, context, elseIfInd, file);
            context.beaconStack.push(elseIfInd.getURI());
            return;
        }

        Matcher elseIfEndMatcher = elseIfEndPattern.matcher(line);
        if (elseIfEndMatcher.find()) {
            context.lastIf = context.beaconStack.pop();
            return;
        }

        Matcher elseMatcher = elsePattern.matcher(line);
        if (elseMatcher.find()) {
            Individual elseInd = createElse(model, context, line_num);
            attachDirectiveToOnt(model, context, elseInd, file);
            context.beaconStack.push(elseInd.getURI());
            return;
        }

        Matcher elseEndMatcher = elseEndPattern.matcher(line);
        if (elseEndMatcher.find()) {
            context.lastIf = "";
            context.beaconStack.pop();
            return;
        }

        Matcher locationMatcher = locationPattern.matcher(line);
        if (locationMatcher.find()) {
            String location = locationMatcher.group(1);
            Individual loc = createLocation(model, context, line_num, location);
            context.currentLocation = loc.getURI();
            attachDirectiveToOnt(model, context, loc, file);
            return;
        }

        Matcher locationEndMatcher = locationEndPattern.matcher(line);
        if (locationEndMatcher.find()) {
            Individual eloc = createEndLocation(model, context, line_num, context.currentLocation);
            context.currentLocation = "";
            attachDirectiveToOnt(model, context, eloc, file);
            return;
        }

        Matcher genericMatcher = genericPattern.matcher(line);
        if (genericMatcher.find()) {
            String name = genericMatcher.group(1);
            String args = genericMatcher.group(2);
            Individual beacon = createBeacon(model, context, line_num, name, args);
            attachDirectiveToOnt(model, context, beacon, file);
            context.beaconStack.push(beacon.getURI());
            return;
        }

        Matcher genericEndMatcher = genericEndPattern.matcher(line);
        if (genericEndMatcher.find()) {
            String beacon = genericEndMatcher.group(1);
            try {
                context.beaconStack.pop();
            } catch (EmptyStackException e) {
                System.err.println("Error: ending a beacon " + beacon + " without opening it.");
            }
            return;
        }

        System.err.println("Error: unknown beacon: " + line + " at line " + line_num + " in file " + file.getURI());
    }

    public static void parseDirective(OntModel model, DirectiveContext context, String line, int line_num, Individual file) throws IOException {

        // else and if cannot be separated by a directive
        context.lastIf = "";

        Matcher includeMatcher = includePattern.matcher(line);
        if (includeMatcher.find()) {
            String includedFile = includeMatcher.group(1);
            if (includedFile.contains("*")) {
                List<Path> expanded = expandPath(includedFile);
                // System.out.println("Expanded: " + expanded.toString() + " from " + includedFile);
                expanded.forEach(path -> {
                    try {
                        Individual parsedFile = parseConfigFile(path.toString(), model, model.getBag(OntCWAF.NS + Constants.FILE_BAG_NAME));
                        Individual include = createInclude(model, context, "Include", line_num, parsedFile);
                        attachDirectiveToOnt(model, context, include, file);
                    } catch (NoSuchFileException e) {
                        System.err.println("File not found: " + e.getFile());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                Individual parsedFile = parseConfigFile(includedFile, model, model.getBag(OntCWAF.NS + Constants.FILE_BAG_NAME));
                Individual include = createInclude(model, context, "Include", line_num, parsedFile);
                attachDirectiveToOnt(model, context, include, file);
            }
            return;
        }

        Matcher useMatcher = usePattern.matcher(line);
        if (useMatcher.find()) {
            String macroName = useMatcher.group(1);
            String macroArgs = useMatcher.group(2);
            Individual use = createUse(model, context, line_num, macroArgs == null ? "" : macroArgs, OntUtils.getMacroURI(macroName));
            attachDirectiveToOnt(model, context, use, file);
            // System.out.println("Using macro: " + macroName + " with args: " + macroArgs);
            return;
        }

        Matcher servernameMatcher = servernamePattern.matcher(line); //TODO
        if (servernameMatcher.find()) {
            String serverName = servernameMatcher.group(1);
            context.serverName = serverName;
            return;
        }

        Matcher listenMatcher = listenPattern.matcher(line);
        if (listenMatcher.find()) {
            String listen = listenMatcher.group(1);
            //TODO handle macros arguments
            try {
                context.serverPort = Integer.parseInt(listen);
            } catch (NumberFormatException e) {
                System.err.println("Error: " + listen + " is not yet handled for Listen.");
            }
            return;
        }

        Matcher modSecRuleMatcher = modSecRulePattern.matcher(line);
        if (modSecRuleMatcher.find()) {
            String name = modSecRuleMatcher.group(1);
            String args = modSecRuleMatcher.group(2);
            Matcher phaseMatcher = phasePattern.matcher(args);
            int phase = Constants.DEFAULT_PHASE;
            if (phaseMatcher.find()) {
                phase = Integer.parseInt(phaseMatcher.group(1));
            }
            Individual modSecRule = createModSecRule(model, context, line_num, name, args, phase);
            attachDirectiveToOnt(model, context, modSecRule, file);
            return;
        }

        Matcher generalRuleMatcher = genericRulePattern.matcher(line);
        if (generalRuleMatcher.find()) {
            String ruleKeyword = generalRuleMatcher.group(1);
            String args = generalRuleMatcher.group(2);
            // System.out.println("Rule: " + ruleKeyword + " with args: " + args);
            Individual directiveInd = createRule(model, context, line_num, ruleKeyword, args);//createGeneralDirective(model, context, ruleKeyword, line_num);
            // System.out.println("Adding directive: " + ruleKeyword + " at line " + line_num + " in file " + file.getURI());
            if (directiveInd != null) 
                attachDirectiveToOnt(model, context, directiveInd, file);
            return;
        }

        if (!line.isBlank())
            System.err.println("Error: unknown directive: " + line + " at line " + line_num + " in file " + file.getURI());

    }

    public static void main(String[] args) {
        try {
            OntModel model = parseConfig(args[0]);
            // OntUtils.print_bag(model.getBag(Constants.FILE_BAG_NAME));
            // OntUtils.print_all_statements(model);
            saveOntology("config.ttl", model, "TTL");
            saveOntology("full_schema.ttl", model, "TTL", true);
        } catch (IOException e) {
            e.printStackTrace();
    }

}


    //ChatGPT - Code Snippet
    public static List<Path> expandPath(String pattern) throws IOException {
        Path basePath = Paths.get(pattern).getParent();
        String globPattern = "glob:" + pattern;
        
        if (basePath == null) {
            basePath = Paths.get(".");
        }

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);
        final List<Path> matchedPaths = new ArrayList<>();

        Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (matcher.matches(file)) {
                    matchedPaths.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return matchedPaths;
    }

}