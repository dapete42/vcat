package vcat.cache.file;

import java.nio.file.Path;

import vcat.cache.CacheException;
import vcat.mediawiki.IWiki;
import vcat.params.VCatParams;

public class GraphFileCache<W extends IWiki> extends AbstractFileCache<VCatParams<W>> {

	private static final long serialVersionUID = -5892996784504997579L;

	private static final String PREFIX = "Graph-";

	private static final String SUFFIX = ".gv";

	public GraphFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
	}

}
