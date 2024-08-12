package be.uclouvain.service;

import java.util.regex.Pattern;

public class Constants {
    public static final String FILE_BAG_NAME = "file_bag";
    public static final int DEFAULT_PHASE = 2; //Default phase from ModSecurity

    public class Parser {
        public static Pattern beaconPattern = Pattern.compile("^[ \\t]*<(.*?)>");
        public static Pattern commentPattern = Pattern.compile("^[ \\t]*#");


        // Beacon patterns
        public static Pattern ifPattern = Pattern.compile("[ \\t]*<\\s*If\\s+(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern ifEndPattern = Pattern.compile("[ \\t]*</\\s*If\\s*>", Pattern.CASE_INSENSITIVE);
        public static Pattern ifRulePattern = Pattern.compile("[ \\t]*<\\s*If(.*?)\\s+(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern ifRuleEndPattern = Pattern.compile("[ \\t]*</\\s*If(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern elseIfPattern = Pattern.compile("[ \\t]*<\\s*ElseIf\\s+(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern elseIfEndPattern = Pattern.compile("[ \\t]*</\\s*ElseIf\\s*>", Pattern.CASE_INSENSITIVE);
        public static Pattern elsePattern = Pattern.compile("[ \\t]*<\\s*Else\\s*>", Pattern.CASE_INSENSITIVE);
        public static Pattern elseEndPattern = Pattern.compile("[ \\t]*</\\s*Else\\s*>", Pattern.CASE_INSENSITIVE);
        public static Pattern macroPattern = Pattern.compile("[ \\t]*<\\s*Macro\\s+(\\S+?)(?:\\s+(.*))?>", Pattern.CASE_INSENSITIVE);
        public static Pattern macroEndPattern = Pattern.compile("[ \\t]*</\\s*Macro\\s*>", Pattern.CASE_INSENSITIVE);
        public static Pattern genericPattern = Pattern.compile("[ \\t]*<(?!\\/)\\s*(\\S+?)\\s+(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern genericEndPattern = Pattern.compile("[ \\t]*<\\/(.*?)>", Pattern.CASE_INSENSITIVE);
        // Edge case:
        public static Pattern virtualHostPattern = Pattern.compile("<\\s*VirtualHost\\s+(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern virtualHostEndPattern = Pattern.compile("</\\s*VirtualHost\\s*>", Pattern.CASE_INSENSITIVE);
        public static Pattern locationPattern = Pattern.compile("[ \\t]*<\\s*Location\\s+(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern locationEndPattern = Pattern.compile("[ \\t]*</\\s*Location\\s*>", Pattern.CASE_INSENSITIVE);
        public static Pattern locationMatchPattern = Pattern.compile("[ \\t]*<\\s*LocationMatch\\s+(.*?)>", Pattern.CASE_INSENSITIVE);
        public static Pattern locationMatchEndPattern = Pattern.compile("[ \\t]*</\\s*LocationMatch\\s*>", Pattern.CASE_INSENSITIVE);

        //Directives
        public static Pattern genericRulePattern = Pattern.compile("^[ \\t]*(\\S+)(?:\\s+(.*))?$");
        public static Pattern includePattern = Pattern.compile("^[ \\t]*Include\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
        public static Pattern usePattern = Pattern.compile("^[ \\t]*Use\\s+(\\S+)(?:\\s+(.*))?", Pattern.CASE_INSENSITIVE);
        public static Pattern servernamePattern = Pattern.compile("^[ \\t]*ServerName\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
        public static Pattern listenPattern = Pattern.compile("^[ \\t]*Listen\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
        public static Pattern modSecRulePattern = Pattern.compile("^[ \\t]*(SecRule|SecAction)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        public static Pattern removeByIdPattern = Pattern.compile("^[ \\t]*SecRuleRemoveById\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        public static Pattern removeById_IDPattern = Pattern.compile("\\d+(-\\d+)?", Pattern.CASE_INSENSITIVE);
        public static Pattern removeByTagPattern = Pattern.compile("[ \\t]*SecRuleRemoveByTag\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        public static Pattern phasePattern = Pattern.compile("phase:(\\d+)", Pattern.CASE_INSENSITIVE);
        public static Pattern idPattern = Pattern.compile("id:(\\d+)", Pattern.CASE_INSENSITIVE);
        public static Pattern tagPattern = Pattern.compile("tag:([^,]+)", Pattern.CASE_INSENSITIVE);
        
    }
}
