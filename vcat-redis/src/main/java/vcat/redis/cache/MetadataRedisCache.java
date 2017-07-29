package vcat.redis.cache;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;
import vcat.redis.Messages;

public class MetadataRedisCache extends StringRedisCache implements IMetadataCache {

	private final Log log = LogFactory.getLog(this.getClass());

	public MetadataRedisCache(final JedisPool jedisPool, final String redisPrefix, final int maxAgeInSeconds) {
		super(jedisPool, redisPrefix, maxAgeInSeconds);
	}

	@Override
	public synchronized Metadata getMetadata(IWiki wiki) throws CacheException {
		final String key = wiki.getApiUrl();
		if (this.containsKey(key)) {
			try (Jedis jedis = this.jedisPool.getResource()) {
				final byte[] metadataObjectData = jedis.get(this.jedisKeyBytes(key));
				final Object metadataObject = SerializationUtils.deserialize(metadataObjectData);
				if (metadataObject != null && metadataObject instanceof Metadata) {
					return (Metadata) metadataObject;
				} else {
					// Wrong type
					this.remove(key);
					String message = Messages.getString("MetadataRedisCache.Error.Deserialize");
					log.error(message);
					throw new CacheException(message);
				}
			} catch (SerializationException e) {
				// Error during deserializing
				this.remove(key);
				String message = Messages.getString("MetadataRedisCache.Error.Deserialize");
				log.warn(message, e);
				throw new CacheException(message, e);
			}
		} else {
			return null;
		}
	}

	@Override
	public synchronized void put(IWiki wiki, Metadata metadata) throws CacheException {
		final byte[] keyBytes = this.jedisKeyBytes(wiki.getApiUrl());
		try (Jedis jedis = this.jedisPool.getResource()) {
			Transaction t = jedis.multi();
			t.set(keyBytes, SerializationUtils.serialize(metadata));
			t.expire(keyBytes, this.maxAgeInSeconds);
			t.exec();
		}
	}

}
