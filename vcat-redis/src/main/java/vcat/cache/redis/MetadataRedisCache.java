package vcat.cache.redis;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.JedisPool;

import vcat.cache.CacheException;
import vcat.cache.IMetadataCache;
import vcat.mediawiki.Metadata;

public class MetadataRedisCache extends StringRedisCache implements IMetadataCache {

	private Log log = LogFactory.getLog(this.getClass());

	public MetadataRedisCache(final JedisPool jedisPool, final String redisPrefix, final int maxAgeInSeconds) {
		super(jedisPool, redisPrefix, maxAgeInSeconds);
	}

	public synchronized Metadata getMetadata(String key) throws CacheException {
		if (this.containsKey(key)) {
			final byte[] metadataObjectData = this.jedis.get(this.jedisKeyBytes(key));
			Object metadataObject = SerializationUtils.deserialize(metadataObjectData);
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

	public synchronized void put(String key, Metadata metadata) throws CacheException {
		this.jedis.set(this.jedisKeyBytes(key), SerializationUtils.serialize(metadata));
	}

}
