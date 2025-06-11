package org.toolforge.vcat.mediawiki;

import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.cache.interfaces.MetadataCache;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;

import java.io.Serial;

@Slf4j
public class CachedMetadataProvider implements MetadataProvider {

    @Serial
    private static final long serialVersionUID = 5966985791741996909L;

    private final MetadataCache metadataCache;

    private final MetadataProvider metadataProvider;

    public CachedMetadataProvider(final MetadataProvider metadataProvider, MetadataCache metadataCache) {
        this.metadataProvider = metadataProvider;
        this.metadataCache = metadataCache;
    }

    @Override
    public Metadata requestMetadata(Wiki wiki) throws ApiException {
        Metadata metadata = null;
        try {
            this.metadataCache.purge();
            metadata = this.metadataCache.getMetadata(wiki);
        } catch (CacheException e) {
            LOG.warn(Messages.getString("CachedMetadataProvider.Warn.Retrieve"), e);
        }

        if (metadata == null) {
            try {
                metadata = this.metadataProvider.requestMetadata(wiki);
                this.metadataCache.put(wiki, metadata);
            } catch (CacheException e) {
                throw new ApiException(Messages.getString("CachedMetadataProvider.Warn.Store"), e);
            }
        }

        return metadata;
    }

    public long currentCacheSize() throws CacheException {
        return metadataCache.currentSize();
    }

}
