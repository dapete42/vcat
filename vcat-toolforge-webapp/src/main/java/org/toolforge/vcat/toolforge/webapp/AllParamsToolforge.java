package org.toolforge.vcat.toolforge.webapp;

import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.params.AbstractAllParams;

import java.util.Map;

public class AllParamsToolforge extends AbstractAllParams {

    private final ToolforgeWikiProvider toolforgeMetainfo;

    public AllParamsToolforge(final Map<String, String[]> requestParams, final String renderUrl,
                              final MetadataProvider metadataProvider, final ToolforgeWikiProvider toolforgeMetainfo) throws VCatException {
        this.toolforgeMetainfo = toolforgeMetainfo;
        init(requestParams, renderUrl, metadataProvider);
    }

    @Override
    protected ToolforgeWiki initWiki(String wikiString) throws VCatException {
        return this.toolforgeMetainfo.fromDbname(wikiString);
    }

}