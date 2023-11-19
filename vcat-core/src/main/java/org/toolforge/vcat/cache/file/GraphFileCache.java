package org.toolforge.vcat.cache.file;

import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.params.VCatParams;

import java.nio.file.Path;

public class GraphFileCache extends AbstractFileCache<VCatParams> {

    private static final String PREFIX = "Graph-";

    private static final String SUFFIX = ".gv";

    public GraphFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

}
