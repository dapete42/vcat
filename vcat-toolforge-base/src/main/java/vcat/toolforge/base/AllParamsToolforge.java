package vcat.toolforge.base;

import java.util.Map;

import vcat.VCatException;
import vcat.mediawiki.IMetadataProvider;
import vcat.params.AbstractAllParams;

public class AllParamsToolforge extends AbstractAllParams<ToolforgeWiki> {

	private final ToolforgeWikiProvider toolforgeMetainfo;

	public AllParamsToolforge(final Map<String, String[]> requestParams, final String renderUrl,
			final IMetadataProvider metadataProvider, final ToolforgeWikiProvider toolforgeMetainfo) throws VCatException {
		this.toolforgeMetainfo = toolforgeMetainfo;
		init(requestParams, renderUrl, metadataProvider);
	}

	@Override
	protected ToolforgeWiki initWiki(String wikiString) throws VCatException {
		return this.toolforgeMetainfo.fromDbname(wikiString);
	}

}
