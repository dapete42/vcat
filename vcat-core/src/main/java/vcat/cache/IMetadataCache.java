package vcat.cache;

import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

public interface IMetadataCache {

	Metadata getMetadata(IWiki wiki) throws CacheException;

	void purge();

	void put(IWiki wiki, Metadata metadata) throws CacheException;

}