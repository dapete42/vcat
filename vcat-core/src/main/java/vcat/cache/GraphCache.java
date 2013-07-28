package vcat.cache;

import java.io.File;

import vcat.params.VCatParams;

public class GraphCache extends AbstractFileCache<VCatParams> {

	private final static String PREFIX = "Graph-";

	private final static String SUFFIX = ".gv";

	public GraphCache(File cacheDirectory) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX);
	}

}
