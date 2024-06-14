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
import org.apache.jena.sparql.pfunction.library.alt;

import be.uclouvain.utils.Directive;
import be.uclouvain.utils.OntUtils;

import static be.uclouvain.utils.OntUtils.*;
import be.uclouvain.vocabulary.OntCWAF;
import static be.uclouvain.utils.DirectiveFactory.*;

public class Parser {

    static String pwd = null;

    public static OntModel parseConfig(String filePath) throws IOException {

        OntModel confModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        
        Individual config = confModel.createIndividual(OntCWAF.NS + "config", OntCWAF.CONFIGURATION);
        config.addProperty(OntCWAF.CONFIG_NAME, "first conf");
        
        Bag file_bag = confModel.createBag(OntCWAF.NS + Constants.FILE_BAG_NAME);
        config.addProperty(OntCWAF.CONTAINS_FILE, file_bag);

        parseConfigFile(filePath, confModel, file_bag);

        return confModel;
    }

    private static Individual parseConfigFile(String filePath, OntModel model, Bag file_bag) throws IOException{
        //TODO handle "\" for multiline directives
        Pattern beaconPattern = Pattern.compile("^[ \\t]*<(.*?)>");
        Pattern commentPattern = Pattern.compile("^[ \\t]*#");

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

        Individual file = model.createIndividual(OntCWAF.NS + filePath, OntCWAF.FILE);
        file.addLiteral(OntCWAF.FILE_PATH, filePath);
        file_bag.add(file);

        List<String> lines = Files.readAllLines(Paths.get(pathAdapter(filePath)));

        Context context = new Context();

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

    public static void parseBeacon(OntModel model, Context context, String line, int line_num, Individual file) {
        Pattern virtualHostPattern = Pattern.compile("<VirtualHost\\s+(.*?)>");
        Pattern virtualHostEndPattern = Pattern.compile("</VirtualHost>");
        Pattern locationPattern = Pattern.compile("[ \\t]*<Location\\s+(.*?)>");
        Pattern locationEndPattern = Pattern.compile("[ \\t]*</Location>");
        Pattern ifPattern = Pattern.compile("[ \\t]*<If\\s+(.*?)>");
        Pattern ifPatternEnd = Pattern.compile("[ \\t]*</If>");
        Pattern macroPattern = Pattern.compile("[ \\t]*<Macro\\s+(\\S+?)(?:\\s+.*)?>");
        Pattern macroEndPattern = Pattern.compile("[ \\t]*</Macro>");

        Matcher virtualHostMatcher = virtualHostPattern.matcher(line);
        if (virtualHostMatcher.find()) {
            String virtualHost = virtualHostMatcher.group(1);
            context.currentVirtualhost = virtualHost;
            // System.out.println("VirtualHost: " + virtualHost);
        }

        Matcher virtualHostEndMatcher = virtualHostEndPattern.matcher(line);
        if (virtualHostEndMatcher.find()) {
            context.currentVirtualhost = "";
            // System.out.println("End VirtualHost");
        }

        Matcher macroMatcher = macroPattern.matcher(line);
        if (macroMatcher.find()) {
            String macroName = macroMatcher.group(1);
            Individual macro = createMacro(model, context, line_num, macroName);
            attachDirectiveToOnt(model, context, macro, file);
            context.beaconStack.push(macro.getURI());
            // System.out.println("Macro: " + macroName);
        }

        Matcher macroEndMatcher = macroEndPattern.matcher(line);
        if (macroEndMatcher.find()) {
            try {
                context.beaconStack.pop();
            } catch (EmptyStackException e) {
                System.err.println("Error: ending a macro without opening it.");
            }
            // System.out.println("End Macro");
        }

        Matcher ifMatcher = ifPattern.matcher(line);
        if (ifMatcher.find()) {
            String condition = ifMatcher.group(1);
            Individual ifInd = createIf(model, context, line_num, condition);
        attachDirectiveToOnt(model, context, ifInd, file);
            context.beaconStack.push(ifInd.getURI());
            // System.out.println("If");
        }

        Matcher ifEndMatcher = ifPatternEnd.matcher(line);
        if (ifEndMatcher.find()) {
            context.beaconStack.pop();
            // System.out.println("End If");
        }

        Matcher locationMatcher = locationPattern.matcher(line);
        if (locationMatcher.find()) {
            String location = locationMatcher.group(1);
            context.currentLocation = location;
            // System.out.println("Location: " + location);
        }

        Matcher locationEndMatcher = locationEndPattern.matcher(line);
        if (locationEndMatcher.find()) {
            context.currentLocation = "";
            // System.out.println("End Location");
        }
    }

    public static void parseDirective(OntModel model, Context context, String line, int line_num, Individual file) throws IOException {

        // Pattern genericRulePattern = Pattern.compile("^[ \\t]*(\\S+|\".*\")\\s+(\\S+|\".*\")\\s+(\\S+|\".*\")$"); //TODO is it enough ?
        Pattern genericRulePattern = Pattern.compile("^[ \\t]*(\\S+)\\s+(.*)$");
        Pattern includePattern = Pattern.compile("^[ \\t]*Include\\s+(\\S+)");
        Pattern usePattern = Pattern.compile("^[ \\t]*Use\\s+(\\S+)(?:\\s+(.*))?");
        Pattern servernamePattern = Pattern.compile("^[ \\t]*ServerName\\s+(\\S+)");
        Pattern listenPattern = Pattern.compile("^[ \\t]*Listen\\s+(\\S+)");
        Pattern modSecRulePattern = Pattern.compile("^[ \\t]*(SecRule|SecAction)\\s+(.*)$");
        Pattern phasePattern = Pattern.compile("phase:(\\d+)");

        Matcher includeMatcher = includePattern.matcher(line);
        if (includeMatcher.find()) {
            String includedFile = includeMatcher.group(1);
            if (includedFile.contains("*")) {
                List<Path> expanded = expandPath(pathAdapter(includedFile));
                // System.out.println("Expanded: " + expanded.toString() + " from " + includedFile);
                expanded.forEach(path -> {
                    try {
                        Individual parsedFile = parseConfigFile(path.toString(), model, model.getBag(Constants.FILE_BAG_NAME));
                        Individual include = createInclude(model, context, "Include", line_num, parsedFile);
                        attachDirectiveToOnt(model, context, include, file);
                    } catch (NoSuchFileException e) {
                        System.err.println("File not found: " + path.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                Individual parsedFile = parseConfigFile(includedFile, model, model.getBag(Constants.FILE_BAG_NAME));
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

        Matcher servernameMatcher = servernamePattern.matcher(line);
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
            if (phaseMatcher.find()) {
                String phase = phaseMatcher.group(1);
                Individual modSecRule = createModSecRule(model, context, line_num, name, args, Integer.parseInt(phase));
                attachDirectiveToOnt(model, context, modSecRule, file);
                // System.out.println("Found phase: " + Integer.parseInt(phase) + " for: " + name + " " + args);
                return;
            }
        }

        Matcher generalRuleMatcher = genericRulePattern.matcher(line);
        if (generalRuleMatcher.find()) {
            //TODO change to match any Rule
            String ruleKeyword = generalRuleMatcher.group(1);
            String args = generalRuleMatcher.group(2);
            // System.out.println("Rule: " + ruleKeyword + " with args: " + args);
            Individual directiveInd = createGeneralDirective(model, context, ruleKeyword, line_num);
            // System.out.println("Adding directive: " + ruleKeyword + " at line " + line_num + " in file " + file.getURI());
            if (directiveInd != null) 
                attachDirectiveToOnt(model, context, directiveInd, file);
            return;
        }

    }

    public static void main(String[] args) {
        pwd = args[0];
        try {
            OntModel model = parseConfig(args[1]);
            // OntUtils.print_bag(model.getBag(Constants.FILE_BAG_NAME));
            // OntUtils.print_all_statements(model);
            saveOntology("config.ttl", model, "TTL");
        } catch (IOException e) {
            e.printStackTrace();
    }

}

    private static String pathAdapter(String path) {
        return path.replace("conf/", pwd+"/");
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