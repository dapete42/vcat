package vcat.cache.interfaces;

import vcat.cache.CacheException;
import vcat.mediawiki.Metadata;
import vcat.mediawiki.interfaces.Wiki;

public interface MetadataCache {

    Metadata getMetadata(Wiki wiki) throws CacheException;

    void purge() throws CacheException;

    void put(Wiki wiki, Metadata metadata) throws CacheException;

}