package vcat.cache.file;

import vcat.cache.CacheException;
import vcat.mediawiki.interfaces.Wiki;
import vcat.params.CombinedParams;

import java.nio.file.Path;

public class RenderedFileCache<W extends Wiki> extends AbstractFileCache<CombinedParams<W>> {

    private static final String PREFIX = "RenderedFile-";

    private static final String SUFFIX = "";

    public RenderedFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

    @Override
    protected String getCacheFilename(CombinedParams<W> key) {
        // Append file extension to generated file names to make it easier to recognize to humans
        return super.getCacheFilename(key) + '.' + key.getGraphviz().getOutputFormat().getFileExtension();
    }

}
