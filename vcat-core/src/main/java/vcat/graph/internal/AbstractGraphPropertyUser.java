package vcat.graph.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all classes that use the {@link GraphProperty} annotation.
 *
 * @author Peter Schlömer
 */
public abstract class AbstractGraphPropertyUser {

    /**
     * @return Property map, containing the values returned by all getter methods marked with the {@link GraphProperty}
     * annotation.
     */
    public Map<String, String> properties() {
        Map<String, String> properties = new HashMap<>();
        for (Method method : this.getClass().getMethods()) {
            if (method.isAnnotationPresent(GraphProperty.class)) {
                Object valueObject;
                try {
                    valueObject = method.invoke(this);
                    if (valueObject != null) {
                        String name = method.getAnnotation(GraphProperty.class).value();
                        if (name != null) {
                            properties.put(name, valueObject.toString());
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    // ignore
                }
            }
        }
        return properties;
    }

}
