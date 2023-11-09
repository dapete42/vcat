package vcat.test;

import vcat.mediawiki.Metadata;
import vcat.params.AbstractAllParams;

public class TestAllParams extends AbstractAllParams<TestWiki> {

    @Override
    public TestWiki initWiki(String wikiString) {
        return new TestWiki();
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public void putRequestParam(String key, String[] value) {
        this.requestParams.put(key, value);
    }

}