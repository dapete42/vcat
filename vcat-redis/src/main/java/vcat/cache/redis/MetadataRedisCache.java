package vcat.cache.redis;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

public class MetadataRedisCache extends StringRedisCache implements IMetadataCache {

	private Log log = LogFactory.getLog(this.getClass());

	public MetadataRedisCache(final JedisPool jedisPool, final String redisPrefix, final int maxAgeInSeconds) {
		super(jedisPool, redisPrefix, maxAgeInSeconds);
	}

	@Override
	public synchronized Metadata getMetadata(IWiki wiki) throws CacheException {
		final String key = wiki.getApiUrl();
		if (this.containsKey(key)) {
			final Jedis jedis = this.jedisPool.getResource();
			final byte[] metadataObjectData = jedis.get(this.jedisKeyBytes(key));
			this.jedisPool.returnResource(jedis);
			Object metadataObject = SerializationUtils.deserialize(metadataObjectData);
			if (metadataObject instanceof Metadata) {
				return (Metadata) metadataObject;
			} else {
				// Wrong type - remove from cache and throw error
				this.remove(key);
				String message = "Error while deserializing cached data to Metadata";
				log.error(message);
				throw new CacheException(message);
			}
		} else {
			return null;
		}
	}

	@Override
	public synchronized void put(IWiki wiki, Metadata metadata) throws CacheException {
		final String key = wiki.getApiUrl();
		final Jedis jedis = this.jedisPool.getResource();
		jedis.set(this.jedisKeyBytes(key), SerializationUtils.serialize(metadata));
		this.jedisPool.returnResource(jedis);
	}

}
