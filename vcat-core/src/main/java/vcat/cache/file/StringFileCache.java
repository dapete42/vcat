package vcat.cache.file;

import java.io.File;

import vcat.cache.CacheException;

public abstract class StringFileCache extends AbstractFileCache<String> {

	protected StringFileCache(final File cacheDirectory, final String prefix, final String suffix,
			final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, prefix, suffix, maxAgeInSeconds);
	}

}
