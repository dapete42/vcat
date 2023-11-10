package vcat.util;

import vcat.mediawiki.Metadata;
import vcat.params.AbstractAllParams;

import java.io.Serial;

/**
 * Link provider which creates links to the corresponding wiki page.
 *
 * @author Peter Schlömer
 */
public class WikiLinkProvider extends AbstractLinkProvider {

    @Serial
    private static final long serialVersionUID = 3854287898146040511L;

    /**
     * Pattern (with $1 placeholder) to use when creating link.
     */
    private final String pattern;

    public WikiLinkProvider(final AbstractAllParams<?> all) {
        // The pattern can be constructed from metadata information
        final Metadata metadata = all.getMetadata();
        String tempPattern = metadata.getServer() + metadata.getArticlepath();
        // If the pattern uses a protocol-relative URI, we need to explicitly set a protocol. This is because graphs are
        // not just displayed in a browser, but will probably also be saved, which breaks the links.
        if (tempPattern.startsWith("//")) {
            tempPattern = "http:" + tempPattern;
        }
        this.pattern = tempPattern;
    }

    @Override
    public String provideLink(final String title) {
        return pattern.replace("$1", escapeMediawikiTitleForUrl(title));
    }

}
