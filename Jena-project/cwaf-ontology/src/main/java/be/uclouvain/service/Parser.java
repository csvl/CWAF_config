package be.uclouvain.service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;

import be.uclouvain.utils.Directive;
import be.uclouvain.utils.OntUtils;
import be.uclouvain.utils.OntUtils.*;
import be.uclouvain.vocabulary.OntCWAF;

public class Parser {

    static String pwd = null;

    public static OntModel parseConfig(String filePath) throws IOException {

        OntModel confModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        
        Individual config = confModel.createIndividual("config", OntCWAF.CONFIGURATION);
        config.addProperty(OntCWAF.CONFIG_NAME, "first conf");
        
        Bag file_bag = confModel.createBag(Constants.FILE_BAG_NAME);

        parseConfigFile(filePath, confModel, file_bag);

        return confModel;
    }

    private static void parseConfigFile(String filePath, OntModel model, Bag file_bag) throws IOException{
        //TODO handle "\" for multiline directives
        Pattern beaconPattern = Pattern.compile("^[ \\t]*<(.*?)>");
        Pattern commentPattern = Pattern.compile("^[ \\t]*#");

        // System.out.println("Including file: " + filePath);

        //Check if file is already in the bag
        //TODO optimize ?
        for (Iterator<RDFNode> i = file_bag.iterator(); i.hasNext();) {
            RDFNode file = i.next();
            if (file.asResource().getURI().equals(filePath)) {
                System.out.println( filePath + " already in the bag.");
                return;
            }
        }

        Individual file = model.createIndividual(filePath, OntCWAF.FILE);
        file.addLiteral(OntCWAF.FILEPATH, filePath);
        file_bag.add(file);

        List<String> lines = Files.readAllLines(Paths.get(pathAdapter(filePath)));

        Context context = new Context();

        for (int line_num = 0; line_num < lines.size(); line_num++){
            String line = lines.get(line_num);

            if (commentPattern.matcher(line).find()) {
                continue;
            }

            Matcher beaconMatcher = beaconPattern.matcher(line);
            if (beaconMatcher.find()) {
                parseBeacon(model, context, line, line_num);
                continue;
            } else {
                parseDirective(model, context, line, line_num, file);
            }

        }
    }

    public static void parseBeacon(OntModel model, Context context, String line, int line_num) {
        Pattern virtualHostPattern = Pattern.compile("<VirtualHost\\s+(.*?)>");
        Pattern virtualHostEndPattern = Pattern.compile("</VirtualHost>");
        Pattern locationPattern = Pattern.compile("[ \\t]*<Location\\s+(.*?)>");
        Pattern locationEndPattern = Pattern.compile("[ \\t]*</Location>");
        Pattern ifPattern = Pattern.compile("[ \\t]*<If\\s+");
        Pattern ifPatternEnd = Pattern.compile("[ \\t]*</If>");
        Pattern macroPattern = Pattern.compile("[ \\t]*<Macro\\s+(\\w+)(?:\\s+.*)?>");
        Pattern macroEndPattern = Pattern.compile("[ \\t]*</Macro>");

        Matcher virtualHostMatcher = virtualHostPattern.matcher(line);
        if (virtualHostMatcher.find()) {
            String virtualHost = virtualHostMatcher.group(1);
            context.currentVirtualhost = virtualHost;
            System.out.println("VirtualHost: " + virtualHost);
        }

        Matcher virtualHostEndMatcher = virtualHostEndPattern.matcher(line);
        if (virtualHostEndMatcher.find()) {
            context.currentVirtualhost = "";
            System.out.println("End VirtualHost");
        }

        Matcher macroMatcher = macroPattern.matcher(line);
        if (macroMatcher.find()) {
            String macroName = macroMatcher.group(1);
            context.currentMacroStack.push(macroName);
            System.out.println("Macro: " + macroName);
        }

        Matcher macroEndMatcher = macroEndPattern.matcher(line);
        if (macroEndMatcher.find()) {
            context.currentMacroStack.pop();
            System.out.println("End Macro");
        }

        Matcher ifMatcher = ifPattern.matcher(line);
        if (ifMatcher.find()) {
            context.currentIfStack.push("IF"); //TODO identify ifs ?
            System.out.println("If");
        }

        Matcher ifEndMatcher = ifPatternEnd.matcher(line);
        if (ifEndMatcher.find()) {
            context.currentIfStack.pop();
            System.out.println("End If");
        }

        Matcher locationMatcher = locationPattern.matcher(line);
        if (locationMatcher.find()) {
            String location = locationMatcher.group(1);
            context.currentLocation = location;
            System.out.println("Location: " + location);
        }

        Matcher locationEndMatcher = locationEndPattern.matcher(line);
        if (locationEndMatcher.find()) {
            context.currentLocation = "";
            System.out.println("End Location");
        }
    }

    public static void parseDirective(OntModel model, Context context, String line, int line_num, Individual file) throws IOException {

        Pattern genericRulePattern = Pattern.compile("^[ \\t]*(\\S+|\".*\")\\s+(\\S+|\".*\")\\s+(\\S+|\".*\")$");
        Pattern includePattern = Pattern.compile("^[ \\t]*Include\\s+(\\S+)");
        Pattern usePattern = Pattern.compile("^[ \\t]*Use\\s+(\\S+)(?:\\s+(.*))?");

        Matcher includeMatcher = includePattern.matcher(line);
        if (includeMatcher.find()) {
            String includedFile = includeMatcher.group(1);
            if (includedFile.contains("*")) {
                List<Path> expanded = expandPath(pathAdapter(includedFile));
                // System.out.println("Expanded: " + expanded.toString() + " from " + includedFile);
                expanded.forEach(path -> {
                    try {
                        parseConfigFile(path.toString(), model, model.getBag(Constants.FILE_BAG_NAME));
                    } catch (NoSuchFileException e) {
                        System.err.println("File not found: " + path.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                parseConfigFile(includedFile, model, model.getBag(Constants.FILE_BAG_NAME));
            }
            return;
        }

        Matcher useMatcher = usePattern.matcher(line);
        if (useMatcher.find()) {
            String macroName = useMatcher.group(1);
            String macroArgs = useMatcher.group(2);
            //TODO
            System.out.println("Using macro: " + macroName + " with args: " + macroArgs);
            return;
        }

        Matcher generalRuleMatcher = genericRulePattern.matcher(line);
        if (generalRuleMatcher.find()) {
            //TODO change to match any Rule
            String directive = generalRuleMatcher.group(1);
            String location = generalRuleMatcher.group(2);
            String virtualHost = generalRuleMatcher.group(3);
            Individual directiveInd = createDirective(model, context, directive, line_num);
            // System.out.println("Adding directive: " + directive + " at line " + line_num + " in file " + file.getURI());
            if (directiveInd != null) 
                file.addProperty(OntCWAF.CONTAINS_DIRECTIVE, directiveInd);
            return;
        }

    }

    private static Individual createDirective(OntModel model, Context context, String name, int line_num) {
        if (context.currentMacroStack.size() > 0) {
            // return createMacro(model, context);
            //TODO
            return null;
        } else {
            return createRule(model, context, name, line_num);
        }
    }

    private static Individual createRule(OntModel model, Context context, String name, int line_num) {
        Individual directiveInd = model.createIndividual(name, OntCWAF.RULE);
        directiveInd.addLiteral(OntCWAF.LINE_NUM, line_num);
        directiveInd.addProperty(OntCWAF.HAS_SCOPE,
                model.createIndividual(name+"-scope", OntCWAF.SCOPE)
                    .addProperty(OntCWAF.HAS_LOCATION, 
                        model.createIndividual(name+"-scope-location", OntCWAF.LOCATION)
                            .addLiteral(OntCWAF.LOCATION_PATH, context.currentLocation)
                    ).addProperty(OntCWAF.HAS_VIRTUAL_HOST,
                        model.createIndividual(name+"-scope-vhost", OntCWAF.VIRTUAL_HOST)
                            .addLiteral(OntCWAF.VIRTUAL_HOST_NAME, context.currentVirtualhost)
                    ));
        return directiveInd;
    }

    private static Individual createMacro(OntModel model, Context context) {
        //TODO
        return null;
    }

    public static void main(String[] args) {
        pwd = args[0];
        try {
            OntModel model = parseConfig(args[1]);
            // OntUtils.print_bag(model.getBag(Constants.FILE_BAG_NAME));
            // OntUtils.print_all_statements(model);
            OntUtils.saveOntology("config.ttl", model, "TTL");
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