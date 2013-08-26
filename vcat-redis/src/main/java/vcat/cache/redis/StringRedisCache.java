package vcat.cache.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class StringRedisCache {

	protected final Jedis jedis;

	protected final int maxAgeInSeconds;

	protected final String redisPrefix;

	public StringRedisCache(final JedisPool jedisPool, final String redisPrefix, final int maxAgeInSeconds) {
		this.jedis = jedisPool.getResource();
		this.redisPrefix = redisPrefix;
		this.maxAgeInSeconds = maxAgeInSeconds;
	}

	public synchronized boolean containsKey(final String key) {
		return this.jedis.exists(this.jedisKey(key));
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
		this.jedis.del(this.jedisKey(key));
	}

}