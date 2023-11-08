package vcat.cache.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.interfaces.MetadataCache;
import vcat.mediawiki.Metadata;
import vcat.mediawiki.interfaces.Wiki;

import java.nio.file.Path;

@Slf4j
public class MetadataFileCache extends AbstractFileCache<String> implements MetadataCache {

    private static final String PREFIX = "Metadata-";

    private static final String SUFFIX = "";

    public MetadataFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

    @Override
    public synchronized Metadata getMetadata(Wiki wiki) throws CacheException {
        final String key = wiki.getApiUrl();
        if (this.containsKey(key)) {
            try {
                final Object metadataObject = SerializationUtils.deserialize(this.get(key));
                if (metadataObject == null) {
                    return null;
                } else if (metadataObject instanceof Metadata metadata) {
                    return metadata;
                } else {
                    // Wrong type
                    this.remove(key);
                    String message = Messages.getString("MetadataFileCache.Error.Deserialize");
                    LOG.error(message);
                    throw new CacheException(message);
                }
            } catch (SerializationException e) {
                // Error during deserializing
                this.remove(key);
                throw new CacheException(Messages.getString("MetadataFileCache.Error.Deserialize"), e);
            }
        } else {
            return null;
        }
    }

    @Override
    public synchronized void put(Wiki wiki, Metadata metadata) throws CacheException {
        final String key = wiki.getApiUrl();
        this.put(key, SerializationUtils.serialize(metadata));
    }

}
