package vcat.mediawiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;

public class CachedMetadataProvider implements IMetadataProvider {

	private static final long serialVersionUID = 5966985791741996909L;

	/** Log4j2 Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(CachedMetadataProvider.class);

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
			LOGGER.warn(Messages.getString("CachedMetadataProvider.Warn.Retrieve"), e);
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
