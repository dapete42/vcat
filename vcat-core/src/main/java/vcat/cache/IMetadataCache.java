package vcat.cache;

import vcat.mediawiki.Metadata;

public interface IMetadataCache {

	Metadata getMetadata(String key) throws CacheException;

	void purge();

	void put(String key, Metadata metadata) throws CacheException;

}