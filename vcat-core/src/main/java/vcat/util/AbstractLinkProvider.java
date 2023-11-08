package vcat.util;

import vcat.graph.Node;
import vcat.params.AbstractAllParams;

import java.io.Serial;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Abstract base class for link providers, used to make nodes on the graph link to various things.
 *
 * @author Peter Schlömer
 */
public abstract class AbstractLinkProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 6171390626471214834L;

    protected static String escapeForUrl(final String string) {
        if (string == null) {
            return null;
        }
        try {
            return URLEncoder.encode(string.replace(' ', '_'), "UTF8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    protected static String escapeMediawikiTitleForUrl(final String title) {
        return title == null ? null : escapeForUrl(title).replace("%3A", ":");
    }

    /**
     * @param all Parameters.
     * @return Link provider fitting for the requested parameters.
     */
    public static AbstractLinkProvider fromParams(final AbstractAllParams<?> all) {
        switch (all.getVCat().getLinks()) {
            case Graph:
                return new VCatLinkProvider(all);
            case Wiki:
                return new WikiLinkProvider(all);
            default:
                return new EmptyLinkProvider();
        }
    }

    /**
     * Add a link (<code>href</code> attribute) to the supplied node.
     *
     * @param node  Node in graph.
     * @param title Title to be linked.
     */
    public void addLinkToNode(final Node node, final String title) {
        final String href = this.provideLink(title);
        if (href != null && !href.isEmpty()) {
            node.setHref(href);
        }
    }

    /**
     * @param title Title of wiki page.
     * @return Link to the specified title, or null if no link should be included.
     */
    public abstract String provideLink(final String title);

}
