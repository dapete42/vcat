package vcat.toolforge.webapp;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {

    private static final String BUNDLE_NAME = "vcat.toolforge.webapp.messages";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private static final String RESOURCE_PREFIX_CATGRAPHCONVERTER = "ToolforgeCatgraphConverterServlet.String.";

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    private final String lang;

    public Messages(String lang) {
        this.lang = lang;
    }

    public String getCatgraphConverterString(String key) {
        // ToolforgeCatgraphConverterServlet.String.de.title
        try {
            return RESOURCE_BUNDLE.getString(RESOURCE_PREFIX_CATGRAPHCONVERTER + this.lang + '.' + key);
        } catch (MissingResourceException e1) {
            try {
                return RESOURCE_BUNDLE.getString(RESOURCE_PREFIX_CATGRAPHCONVERTER + "en." + key);
            } catch (MissingResourceException e2) {
                return '!' + key + '!';
            }
        }
    }

}
