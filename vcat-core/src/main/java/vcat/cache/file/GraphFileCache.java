package vcat.cache.file;

import java.io.File;

import vcat.cache.CacheException;
import vcat.mediawiki.IWiki;
import vcat.params.VCatParams;

public class GraphFileCache<W extends IWiki> extends AbstractFileCache<VCatParams<W>> {

	private final static String PREFIX = "Graph-";

	private final static String SUFFIX = ".gv";

	public GraphFileCache(final File cacheDirectory, final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
	}

}
