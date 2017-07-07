package vcat.redis.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class StringRedisCache {

	protected final JedisPool jedisPool;

	protected final int maxAgeInSeconds;

	protected final String redisPrefix;

	public StringRedisCache(final JedisPool jedisPool, final String redisPrefix, final int maxAgeInSeconds) {
		this.jedisPool = jedisPool;
		this.redisPrefix = redisPrefix;
		this.maxAgeInSeconds = maxAgeInSeconds;
	}

	public synchronized boolean containsKey(final String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			return jedis.exists(this.jedisKey(key));
		}
	}

	protected String jedisKey(final String key) {
		return this.redisPrefix + key;
	}

	protected synchronized byte[] jedisKeyBytes(final String key) {
		return this.jedisKey(key).getBytes();
	}

	public void purge() {
		// Do nothing, this is handled by redis itself
	}

	public synchronized void remove(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			jedis.del(this.jedisKey(key));
		}
	}

}