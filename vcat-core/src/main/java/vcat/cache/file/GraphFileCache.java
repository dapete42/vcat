package vcat.cache.file;

import vcat.cache.CacheException;
import vcat.params.VCatParams;

import java.nio.file.Path;

public class GraphFileCache extends AbstractFileCache<VCatParams> {

    private static final String PREFIX = "Graph-";

    private static final String SUFFIX = ".gv";

    public GraphFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

}
