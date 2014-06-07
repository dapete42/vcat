package vcat.toollabs.params;

import java.util.Map;

import vcat.VCatException;
import vcat.mediawiki.IMetadataProvider;
import vcat.params.AbstractAllParams;
import vcat.toollabs.ToollabsWiki;
import vcat.toollabs.ToollabsWikiProvider;

public class AllParamsToollabs extends AbstractAllParams<ToollabsWiki> {

	private final ToollabsWikiProvider toollabsMetainfo;

	public AllParamsToollabs(final Map<String, String[]> requestParams, final IMetadataProvider metadataProvider,
			final ToollabsWikiProvider toollabsMetainfo) throws VCatException {
		this.toollabsMetainfo = toollabsMetainfo;
		init(requestParams, metadataProvider);
	}

	@Override
	protected ToollabsWiki initWiki(String wikiString) throws VCatException {
		return this.toollabsMetainfo.fromDbname(wikiString);
	}

}
