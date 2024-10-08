package org.toolforge.vcat.util;

import lombok.Getter;
import org.toolforge.vcat.params.AbstractAllParams;

import java.io.Serial;

/**
 * Link provider which creates links back to another vCat graph.
 *
 * @author Peter Schlömer
 */
public class VCatLinkProvider extends AbstractLinkProvider {

    @Serial
    private static final long serialVersionUID = -6540033060770454452L;

    @Getter
    private final String renderUrl;

    private final String renderParams;

    protected VCatLinkProvider(final AbstractAllParams all, final String renderUrl) {
        this.renderUrl = renderUrl;
        // Use request map to build the URL string to use
        final StringBuilder renderParamsBuilder = new StringBuilder();
        for (var entry : all.getRequestParams().entrySet()) {
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
        renderParams = renderParamsBuilder.toString();
    }

    public VCatLinkProvider(final AbstractAllParams all) {
        this(all, all.getRenderUrl());
    }

    @Override
    public String provideLink(final String title) {
        return renderUrl + "?title=" + escapeMediawikiTitleForUrl(title) + renderParams;
    }

}
