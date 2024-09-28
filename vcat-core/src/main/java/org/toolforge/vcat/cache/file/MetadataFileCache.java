package org.toolforge.vcat.cache.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.cache.interfaces.MetadataCache;
import org.toolforge.vcat.mediawiki.Metadata;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;

import java.nio.file.Path;

@Slf4j
public class MetadataFileCache extends AbstractFileCache<String> implements MetadataCache {

    private static final String PREFIX = "Metadata-";

    private static final String SUFFIX = "";

    public MetadataFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

    @Override
    @Nullable
    public Metadata getMetadata(Wiki wiki) throws CacheException {
        lock.lock();
        try {
            final String key = wiki.getApiUrl();
            if (containsKey(key)) {
                try {
                    final var metadataObject = SerializationUtils.deserialize(get(key));
                    if (metadataObject == null) {
                        return null;
                    } else if (metadataObject instanceof Metadata metadata) {
                        return metadata;
                    } else {
                        // Wrong type
                        remove(key);
                        String message = Messages.getString("MetadataFileCache.Error.Deserialize");
                        LOG.error(message);
                        throw new CacheException(message);
                    }
                } catch (SerializationException e) {
                    // Error during deserializing
                    remove(key);
                    throw new CacheException(Messages.getString("MetadataFileCache.Error.Deserialize"), e);
                }
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Wiki wiki, Metadata metadata) throws CacheException {
        lock.lock();
        try {
            final String key = wiki.getApiUrl();
            put(key, SerializationUtils.serialize(metadata));
        } finally {
            lock.unlock();
        }
    }

}
