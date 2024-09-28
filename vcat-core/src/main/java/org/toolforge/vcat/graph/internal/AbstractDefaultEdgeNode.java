package org.toolforge.vcat.graph.internal;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.toolforge.vcat.graph.Graph;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Base class for all edges and nodes in a {@link Graph}.
 *
 * @author Peter Schlömer
 */
@Getter
@Setter
public abstract class AbstractDefaultEdgeNode extends AbstractHasGraphProperties {

    /**
     * Label font family name.
     */
    private String fontname;

    /**
     * Label font size
     */
    private int fontsize;

    /**
     * URL in imagemap, PostScript and SVG files.
     */
    private String href;

    private String style;

    @Override
    protected SortedMap<String, String> propertiesInternal() {
        final SortedMap<String, String> properties = new TreeMap<>();
        properties.put(Graph.PROPERTY_FONTNAME, fontname);
        if (fontsize != 0) {
            properties.put(Graph.PROPERTY_FONTSIZE, Integer.toString(fontsize));
        }
        properties.put(Graph.PROPERTY_HREF, href);
        properties.put(Graph.PROPERTY_STYLE, style);
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AbstractDefaultEdgeNode n) {
            return new EqualsBuilder().append(fontname, n.fontname).append(fontsize, n.fontsize).append(href, n.href)
                    .append(style, n.style).build();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(277, 2009).append(fontname).append(fontsize).append(href).append(style).build();
    }

}
