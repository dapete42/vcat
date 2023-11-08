package vcat.cache.file;

import vcat.cache.CacheException;
import vcat.mediawiki.interfaces.Wiki;
import vcat.params.VCatParams;

import java.nio.file.Path;

public class GraphFileCache<W extends Wiki> extends AbstractFileCache<VCatParams<W>> {

    private static final String PREFIX = "Graph-";

    private static final String SUFFIX = ".gv";

    public GraphFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

}
