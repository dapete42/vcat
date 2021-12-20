package vcat.cache.file;

import java.nio.file.Path;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

public class MetadataFileCache extends AbstractFileCache<String> implements IMetadataCache {

	private static final long serialVersionUID = 8857473945212943389L;

	/** Log4j2 Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataFileCache.class);

	private static final String PREFIX = "Metadata-";

	private static final String SUFFIX = "";

	public MetadataFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
	}

	@Override
	public synchronized Metadata getMetadata(IWiki wiki) throws CacheException {
		final String key = wiki.getApiUrl();
		if (this.containsKey(key)) {
			Object metadataObject = null;
			try {
				metadataObject = SerializationUtils.deserialize(this.get(key));
				if (metadataObject == null) {
					return null;
				} else if (metadataObject instanceof Metadata) {
					return (Metadata) metadataObject;
				} else {
					// Wrong type
					this.remove(key);
					String message = Messages.getString("MetadataFileCache.Error.Deserialize");
					LOGGER.error(message);
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
	public synchronized void put(IWiki wiki, Metadata metadata) throws CacheException {
		final String key = wiki.getApiUrl();
		this.put(key, SerializationUtils.serialize(metadata));
	}

}
