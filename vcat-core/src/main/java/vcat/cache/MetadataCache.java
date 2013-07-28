package vcat.cache;

import java.io.File;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.mediawiki.ApiClient;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

public class MetadataCache extends StringFileCache {

	private Log log = LogFactory.getLog(this.getClass());

	private final static String PREFIX = "Metadata-";

	private final static String SUFFIX = "";

	public MetadataCache(File cacheDirectory) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX);
	}

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

	public synchronized Metadata getMetadataOrRetrieveFromApi(ApiClient apiClient) throws ApiException, CacheException {
		IWiki wiki = apiClient.getWiki();
		String key = wiki.getApiUrl();
		Metadata metadata = this.getMetadata(key);
		if (metadata == null) {
			metadata = new Metadata(apiClient);
			this.put(key, metadata);
		}
		return metadata;
	}

	public synchronized void put(String key, Metadata metadata) throws CacheException {
		this.put(key, SerializationUtils.serialize(metadata));
	}

}
