package org.toolforge.vcat.toolforge.webapp;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

public class CatgraphConverter {

    private CatgraphConverter() {
    }

    /**
     * Convert parameters understood by the old Catgraph tool to parameters understood by vCat. <br>
     * Only the absolute minimum is done, so invalid parameters may cause different rendering or errors.
     *
     * @param inputParameters Map of parameters as used by {@link HttpServletRequest#getParameterMap()}.
     * @return Map of parameters as used by {@link HttpServletRequest#getParameterMap()}.
     */
    public static Map<String, String[]> convertParameters(final Map<String, String[]> inputParameters) {

        final HashMap<String, String> parameters = new HashMap<>();
        final HashSet<String> cats = new HashSet<>();

        // Copy parameters
        for (Entry<String, String[]> entry : inputParameters.entrySet()) {
            final String key = entry.getKey();
            for (final String value : entry.getValue()) {
                if ("cat".equals(key)) {
                    // cat is saved separately
                    cats.add(value);
                } else {
                    // Some parameters are renamed
                    String newKey = key;
                    if ("d".equals(key)) {
                        newKey = "depth";
                    } else if ("n".equals(key)) {
                        newKey = "limit";
                    }
                    parameters.put(newKey, value);
                }
            }
        }

        if (parameters.containsKey("wiki")) {
            final String lang = parameters.get("lang");
            String wiki = parameters.get("wiki");
            if ("commons".equalsIgnoreCase(wiki) || "meta".equalsIgnoreCase(wiki)) {
                wiki = wiki + "wiki";
            } else if (lang != null) {
                if ("wikipedia".equalsIgnoreCase(wiki)) {
                    wiki = lang + "wiki";
                } else if (wiki.matches("^(wiki(books|news|quote|versity)|wiktionary)$")) {
                    wiki = lang + wiki;
                }
            }
            parameters.put("wiki", wiki);
            parameters.remove("lang");
        }

        // Set defaults for ns if it is not supplied
        if (!parameters.containsKey("ns")) {
            if ("article".equalsIgnoreCase(parameters.get("sub"))) {
                // For sub=article, default is 0
                parameters.put("ns", "0");
            } else {
                // Otherwise, default is 14 (category namespace)
                parameters.put("ns", "14");
            }
        }

        // Set rel=subcategory if sub is set to a generic true value, but not 'article'
        if (parameters.containsKey("sub")) {
            if (!"article".equalsIgnoreCase(parameters.get("sub")) && isPhpTrue(parameters.get("sub"))) {
                parameters.put("rel", "subcategory");
            }
            parameters.remove("sub");
        }

        // Set algorithm=fdp if the fdp parameter is a generic true value
        if (parameters.containsKey("fdp")) {
            if (isPhpTrue(parameters.get("fdp"))) {
                parameters.put("algorithm", "fdp");
            }
            parameters.remove("fdp");
        }

        // If links=wiki, leave it like this; if links is any other true value, use links=graph
        if (parameters.containsKey("links")) {
            final String links = parameters.get("links");
            if ("wiki".equalsIgnoreCase(links)) {
                parameters.put("links", "wiki");
            } else if (isPhpTrue(links)) {
                parameters.put("links", "graph");
            } else {
                parameters.remove("links");
            }
        }

        // format=png is redundant
        if ("png".equalsIgnoreCase(parameters.get("format"))) {
            parameters.remove("format");
        }

        // depth=0 and limit=0 must still be removed
        for (String key : new String[]{"depth", "limit"}) {
            final String value = parameters.get(key);
            try {
                if (value != null && Integer.parseInt(value) == 0) {
                    parameters.remove(key);
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        // showhidden is enabled if ignorehidden was explicitly set to false
        if (parameters.containsKey("ignorehidden")) {
            if (isPhpFalse(parameters.get("ignorehidden"))) {
                parameters.put("showhidden", "1");
            }
            parameters.remove("ignorehidden");
        }

        // small is removed because vCat does not support it
        parameters.remove("small");

        // Rebuild proper request parameters as expected (values are arrays)
        final HashMap<String, String[]> outputParameters = new HashMap<>();

        // Fill parameters category or title
        if (parameters.containsKey("ns") && "14".equals(parameters.get("ns"))) {
            // If ns is not supplied or 14, use cat for category parameters and remove ns
            if (!cats.isEmpty()) {
                outputParameters.put("category", cats.toArray(String[]::new));
            }
            parameters.remove("ns");
        } else {
            // If ns is not 14, we must use title
            if (!cats.isEmpty()) {
                outputParameters.put("title", cats.toArray(String[]::new));
            }
        }

        // Fill in all other parameters
        for (Entry<String, String> entry : parameters.entrySet()) {
            outputParameters.put(entry.getKey(), new String[]{entry.getValue()});
        }

        return outputParameters;
    }

    /**
     * @param s String to evaluate.
     * @return Whether the string would be considered as 'true' value by PHP.
     */
    private static boolean isPhpTrue(final String s) {
        return !isPhpFalse(s);
    }

    /**
     * @param s String to evaluate.
     * @return Whether the string would be considered as 'false' by PHP.
     */
    private static boolean isPhpFalse(final String s) {
        return s == null || s.isEmpty() || "0".equals(s) || "false".equals(s);
    }

}