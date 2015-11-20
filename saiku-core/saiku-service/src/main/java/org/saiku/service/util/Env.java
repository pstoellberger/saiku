package org.saiku.service.util;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Env {

    public static String SAIKU_HOME = "SAIKU_HOME";
    public static String SAIKU_HOME_FOLDER = ".saiku";

    public static String resolve(String content) {
        Map<String, String> parameters = new HashMap<>();
        final Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
        final Matcher m = p.matcher(content);
        while (m.find()) {
            String var = m.group(1);
            String val = System.getenv(var);
            if (val == null) {
                val = System.getProperty(var);
            }
            if (val == null && StringUtils.equals(SAIKU_HOME, var)) {
                val = System.getProperty("user.home") + File.separator + SAIKU_HOME_FOLDER;
            }
            if (val == null) {
                val = "$" + var;
            }
            parameters.put(var, val);
        }
        for (String parameter : parameters.keySet()) {
            String value = Matcher.quoteReplacement(parameters.get(parameter));
            String par = Matcher.quoteReplacement(parameter);
            content = content.replaceAll("(?i)\\$\\{" + par + "\\}", value);
        }
        return content;
    }
}
