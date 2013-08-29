package vcat.toollabs;

import java.util.Map;

import vcat.VCatException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.IWiki;
import vcat.params.AllParams;

public class AllParamsToollabs extends AllParams {

	private final ToollabsMetainfoReader toollabsMetainfo;

	public AllParamsToollabs(final Map<String, String[]> requestParams, final IApiCache apiCache,
			final IMetadataCache metadataCache, final ToollabsMetainfoReader toollabsMetainfo) throws VCatException {
		this.toollabsMetainfo = toollabsMetainfo;
		init(requestParams, apiCache, metadataCache);
	}

	@Override
	protected IWiki initWiki(String wikiString) throws VCatException {
		return this.toollabsMetainfo.wikiFor(wikiString);
	}

}
