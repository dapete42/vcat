package org.toolforge.vcat.params;

import org.jspecify.annotations.Nullable;

/**
 * Links parameter for vCat. Determines whether links are included in the graph, and where they should lead to.
 *
 * @author Peter Schlömer
 */
public enum Links {

    /**
     * No links in graph.
     */
    None,
    /**
     * Include links to another vCat graph, starting from the node.
     */
    Graph,
    /**
     * Include links to wiki article for node.
     */
    Wiki;

    @Nullable
    public static Links valueOfIgnoreCase(String name) {
        for (Links link : values()) {
            if (link.name().equalsIgnoreCase(name)) {
                return link;
            }
        }
        return null;
    }

}
