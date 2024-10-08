package org.toolforge.vcat.util;

import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.graph.Node;
import org.toolforge.vcat.params.AbstractAllParams;

import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for link providers, used to make nodes on the graph link to various things.
 *
 * @author Peter Schlömer
 */
public abstract class AbstractLinkProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 6171390626471214834L;

    @Nullable
    protected static String escapeForUrl(@Nullable String string) {
        if (string == null) {
            return null;
        }
        return URLEncoder.encode(string.replace(' ', '_'), StandardCharsets.UTF_8);
    }

    @Nullable
    protected static String escapeMediawikiTitleForUrl(@Nullable String title) {
        return title == null ? null : escapeForUrl(title).replace("%3A", ":");
    }

    /**
     * @param all Parameters.
     * @return Link provider fitting for the requested parameters.
     */
    public static AbstractLinkProvider fromParams(AbstractAllParams all) {
        return switch (all.getVCat().getLinks()) {
            case Graph -> new VCatLinkProvider(all);
            case Wiki -> new WikiLinkProvider(all);
            default -> new EmptyLinkProvider();
        };
    }

    /**
     * Add a link (<code>href</code> attribute) to the supplied node.
     *
     * @param node  Node in graph.
     * @param title Title to be linked.
     */
    public void addLinkToNode(Node node, String title) {
        final String href = provideLink(title);
        if (href != null && !href.isEmpty()) {
            node.setHref(href);
        }
    }

    /**
     * @param title Title of wiki page.
     * @return Link to the specified title, or null if no link should be included.
     */
    @Nullable
    public abstract String provideLink(String title);

}
