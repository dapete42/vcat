package org.toolforge.vcat.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.toolforge.vcat.cache.interfaces.MetadataCache;
import org.toolforge.vcat.mediawiki.Metadata;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;

import java.time.Duration;

public class MetadataCaffeineCache implements MetadataCache {

    private final Cache<Wiki, Metadata> cache;

    public MetadataCaffeineCache(int size, int timeout) {
        cache = Caffeine.newBuilder()
                .maximumSize(size)
                .expireAfterWrite(Duration.ofSeconds(timeout))
                .build();
    }


    @Override
    public Metadata getMetadata(Wiki wiki) {
        return cache.getIfPresent(wiki);
    }

    @Override
    public void purge() {
        cache.cleanUp();
    }

    @Override
    public void put(Wiki wiki, Metadata metadata) {
        cache.put(wiki, metadata);
    }

}
