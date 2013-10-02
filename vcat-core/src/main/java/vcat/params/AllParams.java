package vcat.params;

import java.util.Map;

import vcat.VCatException;
import vcat.mediawiki.IMetadataProvider;
import vcat.mediawiki.SimpleWikimediaWiki;

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

	public AllParams(final Map<String, String[]> requestParams, final IMetadataProvider metadataProvider)
			throws VCatException {
		this.init(requestParams, metadataProvider);
	}

	@Override
	protected SimpleWikimediaWiki initWiki(final String wikiString) throws VCatException {
		return new SimpleWikimediaWiki(wikiString);
	}

}
