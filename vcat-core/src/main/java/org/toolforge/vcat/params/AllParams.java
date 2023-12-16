package org.toolforge.vcat.params;

import jakarta.ws.rs.core.MultivaluedMap;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.mediawiki.SimpleWikimediaWiki;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;

/**
 * A bundle of {@link CombinedParams} plus temporary objects needed to build the category graph. Can only be constructed
 * from a set of request parameters, such as these passed in a ServletRequest; this is parsed, and parameters etc. are
 * created as necessary.
 *
 * @author Peter Schlömer
 */
public class AllParams extends AbstractAllParams {

    public AllParams(final MultivaluedMap<String, String> requestParams, final String renderUrl,
                     final MetadataProvider metadataProvider) throws VCatException {
        this.init(requestParams, renderUrl, metadataProvider);
    }

    @Override
    protected SimpleWikimediaWiki initWiki(final String wikiString) {
        return new SimpleWikimediaWiki(wikiString);
    }

}
