package vcat.params;

import vcat.VCatException;
import vcat.mediawiki.SimpleWikimediaWiki;
import vcat.mediawiki.interfaces.MetadataProvider;

import java.util.Map;

/**
 * A bundle of {@link CombinedParams} plus temporary objects needed to build the category graph. Can only be constructed
 * from a set of request parameters, such as these passed in a ServletRequest; this is parsed, and parameters etc. are
 * created as necessary.
 *
 * @author Peter Schlömer
 */
public class AllParams extends AbstractAllParams<SimpleWikimediaWiki> {

    protected AllParams() {
    }

    public AllParams(final Map<String, String[]> requestParams, final String renderUrl,
                     final MetadataProvider metadataProvider) throws VCatException {
        this.init(requestParams, renderUrl, metadataProvider);
    }

    @Override
    protected SimpleWikimediaWiki initWiki(final String wikiString) throws VCatException {
        return new SimpleWikimediaWiki(wikiString);
    }

}
