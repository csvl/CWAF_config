package be.uclouvain.service;

import java.util.regex.Pattern;

public class Constants {
    public static final String FILE_BAG_NAME = "file_bag";
    public static final int DEFAULT_PHASE = 2; //Default phase from ModSecurity

    public class Parser {
        public static Pattern beaconPattern = Pattern.compile("^[ \\t]*<(.*?)>");
        public static Pattern commentPattern = Pattern.compile("^[ \\t]*#");


        // Beacon patterns
        public static Pattern virtualHostPattern = Pattern.compile("<VirtualHost\\s+(.*?)>");
        public static Pattern virtualHostEndPattern = Pattern.compile("</VirtualHost>");
        public static Pattern locationPattern = Pattern.compile("[ \\t]*<Location\\s+(.*?)>");
        public static Pattern locationEndPattern = Pattern.compile("[ \\t]*</Location>");
        public static Pattern ifPattern = Pattern.compile("[ \\t]*<If\\s+(.*?)>");
        public static Pattern ifPatternEnd = Pattern.compile("[ \\t]*</If>");
        public static Pattern elseIfPattern = Pattern.compile("[ \\t]*<ElseIf\\s+(.*?)>");
        public static Pattern elseIfEndPattern = Pattern.compile("[ \\t]*</ElseIf>");
        public static Pattern elsePattern = Pattern.compile("[ \\t]*<Else>");
        public static Pattern elseEndPattern = Pattern.compile("[ \\t]*</Else>");
        public static Pattern macroPattern = Pattern.compile("[ \\t]*<Macro\\s+(\\S+?)(?:\\s+(.*))?>");
        public static Pattern macroEndPattern = Pattern.compile("[ \\t]*</Macro>");
        public static Pattern genericPattern = Pattern.compile("[ \\t]*<(\\S+?)\\s+(.*?)>");
        public static Pattern genericEndPattern = Pattern.compile("[ \\t]*</(.*?)>");

        //Directives
        public static Pattern genericRulePattern = Pattern.compile("^[ \\t]*(\\S+)(?:\\s+(.*))?$");
        public static Pattern includePattern = Pattern.compile("^[ \\t]*Include\\s+(\\S+)");
        public static Pattern usePattern = Pattern.compile("^[ \\t]*Use\\s+(\\S+)(?:\\s+(.*))?");
        public static Pattern servernamePattern = Pattern.compile("^[ \\t]*ServerName\\s+(\\S+)");
        public static Pattern listenPattern = Pattern.compile("^[ \\t]*Listen\\s+(\\S+)");
        public static Pattern modSecRulePattern = Pattern.compile("^[ \\t]*(SecRule|SecAction)\\s+(.*)$");
        public static Pattern phasePattern = Pattern.compile("phase:(\\d+)");
        
    }
}
