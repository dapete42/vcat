package org.toolforge.vcat.cache.interfaces;

import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.mediawiki.Metadata;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;

public interface MetadataCache {

    @Nullable
    Metadata getMetadata(Wiki wiki) throws CacheException;

    void purge() throws CacheException;

    void put(Wiki wiki, Metadata metadata) throws CacheException;

    long currentSize() throws CacheException;

}