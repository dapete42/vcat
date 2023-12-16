package org.toolforge.vcat.test;

import org.toolforge.vcat.mediawiki.Metadata;
import org.toolforge.vcat.params.AbstractAllParams;

import java.util.List;

public class TestAllParams extends AbstractAllParams {

    @Override
    public TestWiki initWiki(String wikiString) {
        return new TestWiki();
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public void putRequestParam(String key, List<String> value) {
        this.requestParams.put(key, value);
    }

}