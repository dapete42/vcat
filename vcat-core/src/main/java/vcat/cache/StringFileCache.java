package vcat.cache;

import java.io.File;

public abstract class StringFileCache extends AbstractFileCache<String> {

	protected StringFileCache(File cacheDirectory, String prefix, String suffix) throws CacheException {
		super(cacheDirectory, prefix, suffix);
	}

	@Override
	protected String hashForKey(String key) {
		// Directly used bytes for hash instead of serializing
		return this.hash(key.getBytes());
	}

}
