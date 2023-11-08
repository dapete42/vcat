package vcat.util;

import vcat.params.AbstractAllParams;

import java.util.Map.Entry;

/**
 * Link provider which creates links back to another vCat graph.
 *
 * @author Peter Schlömer
 */
public class VCatLinkProvider extends AbstractLinkProvider {

    private static final long serialVersionUID = -6540033060770454452L;

    private final String renderUrl;

    private final String renderParams;

    protected VCatLinkProvider(final AbstractAllParams<?> all, final String renderUrl) {
        this.renderUrl = renderUrl;
        // Use request map to build the URL string to use
        final StringBuilder renderParamsBuilder = new StringBuilder();
        for (Entry<String, String[]> entry : all.getRequestParams().entrySet()) {
            final String key = entry.getKey();
            if ("category".equalsIgnoreCase(key) || "ns".equalsIgnoreCase(key) || "title".equalsIgnoreCase(key)) {
                // category, title and ns may must be removed, ignore them here
            } else {
                for (String value : entry.getValue()) {
                    renderParamsBuilder.append("&amp;").append(escapeForUrl(key));
                    if (value != null) {
                        renderParamsBuilder.append('=').append(escapeForUrl(value));
                    }
                }
            }
        }
        this.renderParams = renderParamsBuilder.toString();
    }

    public VCatLinkProvider(final AbstractAllParams<?> all) {
        this(all, all.getRenderUrl());
    }

    public String getRenderUrl() {
        return this.renderUrl;
    }

    @Override
    public String provideLink(final String title) {
        return this.renderUrl + "?title=" + escapeMediawikiTitleForUrl(title) + this.renderParams;
    }

}
