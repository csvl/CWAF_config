package be.uclouvain.utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public static List<Directive> parseCompiledConfig(String filePath) throws IOException {
        List<Directive> directives = new ArrayList<>();

        Pattern serverNamePattern = Pattern.compile("[ \\t]*SetEnv (\\S+)");
        Pattern virtualHostPattern = Pattern.compile("<VirtualHost\\s+(.*?)>");
        Pattern virtualHostEndPattern = Pattern.compile("</VirtualHost>");
        Pattern locationPattern = Pattern.compile("[ \\t]*<Location\\s+(.*?)>");
        Pattern locationEndPattern = Pattern.compile("[ \\t]*</Location>");
        Pattern ifPattern = Pattern.compile("[ \\t]*<If\\s+");
        Pattern ifPatternEnd = Pattern.compile("[ \\t]*</If>");
        Pattern fileFlag = Pattern.compile("# In file:");
        Pattern lineNumbersRegex = Pattern.compile("used on line (\\d+)");
        Pattern macroNamesRegex = Pattern.compile("macro '(\\w+)'");
        Pattern instructionNumberPattern = Pattern.compile("#\\s+(\\d+):");

        List<String> lines = Files.readAllLines(Paths.get(filePath));

        String currentVirtualhost = null;
        String currentLocation = null;
        int currentIfLevel = 0;
        int currentOrderingNumber = 0;
        List<String> currentMacroStack = new ArrayList<>();
        Integer currentInstructionNumber = null;

        for (String line : lines) {
            Matcher matchVirtualHost = virtualHostPattern.matcher(line);
            if (matchVirtualHost.find()) {
                currentVirtualhost = "VHOST";
                currentLocation = null;
                currentIfLevel = 0;
                continue;
            }

            Matcher matchVirtualHostEnd = virtualHostEndPattern.matcher(line);
            if (matchVirtualHostEnd.find()) {
                currentVirtualhost = null;
                continue;
            }

            Matcher matchLocation = locationPattern.matcher(line);
            if (matchLocation.find()) {
                currentLocation = "LOC";
                continue;
            }

            Matcher matchLocationEnd = locationEndPattern.matcher(line);
            if (matchLocationEnd.find()) {
                currentLocation = null;
            }

            if (ifPattern.matcher(line).find()) {
                currentIfLevel++;
            }
            if (ifPatternEnd.matcher(line).find()) {
                currentIfLevel--;
            }

            if (fileFlag.matcher(line).find()) {
                Matcher matcherNumber = lineNumbersRegex.matcher(line);
                Matcher matcherMacroName = macroNamesRegex.matcher(line);
                currentInstructionNumber = Integer.parseInt(matcherNumber.find() ? matcherNumber.group(1) : "0");
                currentMacroStack.add(matcherMacroName.find() ? matcherMacroName.group(1) : null);
            }

            Matcher matchInstructionNumber = instructionNumberPattern.matcher(line);
            if (matchInstructionNumber.find() && currentMacroStack.isEmpty()) {
                currentInstructionNumber = Integer.parseInt(matchInstructionNumber.group(1));
            }

            Matcher matchServerName = serverNamePattern.matcher(line);
            if (matchServerName.find()) {
                String serverName = matchServerName.group(1);
                currentOrderingNumber++;
                Directive newDirective = new Directive(currentLocation, currentVirtualhost, currentIfLevel, currentMacroStack.isEmpty() ? null : currentMacroStack.get(0), currentMacroStack.isEmpty() ? null : currentMacroStack.get(currentMacroStack.size() - 1), currentInstructionNumber, serverName);
                directives.add(newDirective);
            }
        }

        System.out.println(directives);
        directives.sort(null);
        System.err.println(directives);
        return directives;
    }
}