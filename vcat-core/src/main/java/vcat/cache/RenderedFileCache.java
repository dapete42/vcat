package vcat.cache;

import java.io.File;

import vcat.params.CombinedParams;

public class RenderedFileCache extends AbstractFileCache<CombinedParams> {

	private final static String PREFIX = "RenderedFile-";

	private final static String SUFFIX = "";

	public RenderedFileCache(File cacheDirectory) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX);
	}

	@Override
	protected String getCacheFilename(CombinedParams key) {
		// Append file extension to generated file names to make it easier to recognize to humans
		return super.getCacheFilename(key) + '.' + key.getGraphviz().getOutputFormat().getFileExtension();
	}
}
