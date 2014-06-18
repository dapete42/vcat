package vcat.toollabs.base;

import java.util.Map;

import vcat.VCatException;
import vcat.mediawiki.IMetadataProvider;
import vcat.params.AbstractAllParams;

public class AllParamsToollabs extends AbstractAllParams<ToollabsWiki> {

	private final ToollabsWikiProvider toollabsMetainfo;

	public AllParamsToollabs(final Map<String, String[]> requestParams, final String renderUrl,
			final IMetadataProvider metadataProvider, final ToollabsWikiProvider toollabsMetainfo) throws VCatException {
		this.toollabsMetainfo = toollabsMetainfo;
		init(requestParams, renderUrl, metadataProvider);
	}

	@Override
	protected ToollabsWiki initWiki(String wikiString) throws VCatException {
		return this.toollabsMetainfo.fromDbname(wikiString);
	}

}
