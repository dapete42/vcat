package vcat.cache.file;

import java.io.File;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

public class MetadataFileCache extends StringFileCache implements IMetadataCache {

	private Log log = LogFactory.getLog(this.getClass());

	private final static String PREFIX = "Metadata-";

	private final static String SUFFIX = "";

	public MetadataFileCache(final File cacheDirectory, final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
	}

	@Override
	public synchronized Metadata getMetadata(IWiki wiki) throws CacheException {
		final String key = wiki.getApiUrl();
		if (this.containsKey(key)) {
			Object metadataObject = null;
			try {
				metadataObject = SerializationUtils.deserialize(this.get(key));
				if (metadataObject != null && metadataObject instanceof Metadata) {
					return (Metadata) metadataObject;
				} else {
					// Wrong type
					this.remove(key);
					String message = "Error while deserializing cached file to Metadata; removing from cache";
					log.error(message);
					throw new CacheException(message);
				}
			} catch (SerializationException e) {
				// Error during deserializing
				this.remove(key);
				String message = "Error while deserializing cached file to Metadata; removing from cache";
				log.warn(message, e);
				throw new CacheException(message, e);
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
