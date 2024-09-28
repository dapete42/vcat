package org.toolforge.vcat.params;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Algorithm for vCat.
 *
 * @author Peter Schlömer
 */
@AllArgsConstructor
@Getter
public enum Algorithm {

    /**
     * dot draws directed graphs. It works well on DAGs and other graphs that can be drawn as hierarchies. (Text from
     * Graphviz man page)
     */
    DOT("dot"),
    /**
     * fdp draws undirected graphs using a "spring" model. (Text from Graphviz man page)
     */
    FDP("fdp");

    @Nullable
    public static Algorithm valueOfIgnoreCase(String name) {
        for (Algorithm format : values()) {
            if (format.name().equalsIgnoreCase(name)) {
                return format;
            }
        }
        return null;
    }

    /**
     * graphviz program to use when rendering with this algorithm.
     */
    private final String program;

}
