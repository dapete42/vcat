package vcat.mediawiki;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;

public class CachedMetadataProvider implements IMetadataProvider {

	private final Log log = LogFactory.getLog(this.getClass());

	private final IMetadataCache metadataCache;

	private final IMetadataProvider metadataProvider;

	public CachedMetadataProvider(final IMetadataProvider metadataProvider, IMetadataCache metadataCache) {
		this.metadataProvider = metadataProvider;
		this.metadataCache = metadataCache;
	}

	@Override
	public Metadata requestMetadata(IWiki wiki) throws ApiException {
		Metadata metadata = null;
		try {
			this.metadataCache.purge();
			metadata = this.metadataCache.getMetadata(wiki);
		} catch (CacheException e) {
			log.warn(Messages.getString("CachedMetadataProvider.Warn.Retrieve"), e);
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
}
