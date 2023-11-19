package org.toolforge.vcat.cache.file;

import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.params.CombinedParams;

import java.nio.file.Path;

public class RenderedFileCache extends AbstractFileCache<CombinedParams> {

    private static final String PREFIX = "RenderedFile-";

    private static final String SUFFIX = "";

    public RenderedFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

    @Override
    protected String getCacheFilename(CombinedParams key) {
        // Append file extension to generated file names to make it easier to recognize to humans
        return super.getCacheFilename(key) + '.' + key.getGraphviz().getOutputFormat().getFileExtension();
    }

}
