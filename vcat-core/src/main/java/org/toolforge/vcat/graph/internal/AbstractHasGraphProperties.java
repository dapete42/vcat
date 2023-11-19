package org.toolforge.vcat.graph.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Interface for all graph-related classes that have properties.
 *
 * @author Peter Schlömer
 */
public abstract class AbstractHasGraphProperties {

    /**
     * @return Property map
     */
    public final SortedMap<String, String> properties() {
        final var properties = propertiesInternal();
        final List<String> removeKeys = properties.entrySet().stream()
                .filter(entry -> Objects.isNull(entry.getValue()) || entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();
        removeKeys.forEach(properties::remove);
        return properties;
    }

    protected abstract SortedMap<String, String> propertiesInternal();

}
