package org.toolforge.vcat.toolforge.webapp;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {

    private static final String BUNDLE_NAME = "org.toolforge.vcat.toolforge.webapp.messages";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

}
