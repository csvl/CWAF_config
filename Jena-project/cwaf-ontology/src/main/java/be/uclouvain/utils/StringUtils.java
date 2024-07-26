package be.uclouvain.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    
    public static String[] replaceContentInArray(String[] keys, String[] values, String[] content){
        for (int i = 0; i < content.length; i++) {
            content[i] = replaceContent(keys, values, content[i]);
        }
        return content;
    }

    public static String replaceContent(String[] keys, String[] values, String content){
        for (int i = 0; i < keys.length; i++) {
            String regex = Pattern.quote(keys[i]);
            String localVal = values[i];
            if (localVal != null) {
                content = content.replaceAll(regex, Matcher.quoteReplacement(localVal));
            }
        }
        return content;
    }
}
