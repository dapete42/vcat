package vcat.util;

import java.util.Map.Entry;

import vcat.params.AbstractAllParams;

/**
 * Link provider which creates links back to another vCat graph.
 * 
 * @author Peter Schlömer
 */
public class VCatLinkProvider extends AbstractLinkProvider {

	private static final long serialVersionUID = -6540033060770454452L;

	private final String renderUrl;

	private final String renderParams;

	public VCatLinkProvider(final AbstractAllParams<?> all) {
		this.renderUrl = all.getRenderUrl();
		// Use request map to build the URL string to use
		final StringBuilder renderParams = new StringBuilder();
		for (Entry<String, String[]> entry : all.getRequestParams().entrySet()) {
			final String key = entry.getKey();
			if ("category".equalsIgnoreCase(key) || "ns".equalsIgnoreCase(key) || "title".equalsIgnoreCase(key)) {
				// category, title and ns may must be removed, ignore them here
			} else {
				for (String value : entry.getValue()) {
					renderParams.append('&').append(escapeForUrl(key));
					if (value != null) {
						renderParams.append('=').append(escapeForUrl(value));
					}
				}
			}
		}
		this.renderParams = renderParams.toString();
	}

	public String getRenderUrl() {
		return this.renderUrl;
	}

	@Override
	public String provideLink(final String title) {
		return this.renderUrl + "?title=" + escapeMediawikiTitleForUrl(title) + this.renderParams;
	}

}
