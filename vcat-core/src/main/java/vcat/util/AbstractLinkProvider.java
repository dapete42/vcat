package vcat.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import vcat.graph.Node;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;

/**
 * Abstract base class for link providers, used to make nodes on the graph link to various things.
 * 
 * @author Peter Schlömer
 */
public abstract class AbstractLinkProvider {

	protected static String escapeMediawikiTitleForUrl(final String title) {
		try {
			// Replace blanks by '_', then encode as URL, but allow ':' to remain as is
			return URLEncoder.encode(title.replace(' ', '_'), "UTF8").replaceAll("%3A", ":");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * @param all
	 *            Parameters.
	 * @return Link provider fitting for the requested parameters.
	 */
	public static AbstractLinkProvider fromParams(final AbstractAllParams<? extends IWiki> all) {
		switch (all.getVCat().getLink()) {
		// TODO Links to vCat graphs do not work yet.
		// case Graph:
		// return new VCatLinkProvider();
		case Wiki:
			return new WikiLinkProvider(all.getMetadata());
		default:
			return new EmptyLinkProvider();
		}
	}

	/**
	 * Add a link (<code>href</code> attribute) to the supplied node.
	 * 
	 * @param node
	 *            Node in graph.
	 * @param title
	 *            Title to be linked.
	 */
	public void addLinkToNode(final Node node, final String title) {
		final String href = this.provideLink(title);
		if (href != null && !href.isEmpty()) {
			node.setHref(href);
		}
	}

	/**
	 * @param title
	 *            Title of wiki page.
	 * @return Link to the specified title, or null if no link should be included.
	 */
	public abstract String provideLink(final String title);

}
