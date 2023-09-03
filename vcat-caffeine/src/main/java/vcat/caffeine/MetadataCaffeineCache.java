package vcat.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

import java.time.Duration;

public class MetadataCaffeineCache implements IMetadataCache {

    private final Cache<IWiki, Metadata> cache;

    public MetadataCaffeineCache(int size, int timeout) {
        cache = Caffeine.newBuilder()
                .maximumSize(size)
                .expireAfterWrite(Duration.ofSeconds(timeout))
                .build();
    }


    @Override
    public Metadata getMetadata(IWiki wiki) {
        return cache.getIfPresent(wiki);
    }

    @Override
    public void purge() {
        cache.cleanUp();
    }

    @Override
    public void put(IWiki wiki, Metadata metadata) {
        cache.put(wiki, metadata);
    }

}
