package vcat.cache.file;

import java.io.File;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.Metadata;

public class MetadataFileCache extends StringFileCache implements IMetadataCache {

	private Log log = LogFactory.getLog(this.getClass());

	private final static String PREFIX = "Metadata-";

	private final static String SUFFIX = "";

	public MetadataFileCache(final File cacheDirectory, final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
	}

	@Override
	public synchronized Metadata getMetadata(String key) throws CacheException {
		if (this.containsKey(key)) {
			Object metadataObject = SerializationUtils.deserialize(this.get(key));
			if (metadataObject instanceof Metadata) {
				return (Metadata) metadataObject;
			} else {
				// Wrong type - remove from cache and throw error
				this.remove(key);
				String message = "Error while deserializing cached file to Metadata";
				log.error(message);
				throw new CacheException(message);
			}
		} else {
			return null;
		}
	}

	@Override
	public synchronized void put(String key, Metadata metadata) throws CacheException {
		this.put(key, SerializationUtils.serialize(metadata));
	}

}
