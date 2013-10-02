package vcat.mediawiki;

import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;

public class CachedMetadataProvider implements IMetadataProvider {

	private final IMetadataCache metadataCache;

	private final IMetadataProvider metadataProvider;

	public CachedMetadataProvider(final IMetadataProvider metadataProvider, IMetadataCache metadataCache) {
		this.metadataProvider = metadataProvider;
		this.metadataCache = metadataCache;
	}

	@Override
	public Metadata requestMetadata(IWiki wiki) throws ApiException {
		Metadata metadata;
		try {
			this.metadataCache.purge();
			metadata = this.metadataCache.getMetadata(wiki);
		} catch (CacheException e) {
			throw new ApiException("Error retrieving metadata from cache", e);
		}

		if (metadata == null) {
			try {
				metadata = this.metadataProvider.requestMetadata(wiki);
				this.metadataCache.put(wiki, metadata);
			} catch (CacheException e) {
				throw new ApiException("Error storing metadata in cache", e);
			}
		}

		return metadata;
	}
}
