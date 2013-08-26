package vcat.cache.redis;

import org.json.JSONException;
import org.json.JSONObject;

import redis.clients.jedis.JedisPool;

import vcat.cache.CacheException;
import vcat.cache.IApiCache;

public class ApiRedisCache extends StringRedisCache implements IApiCache {

	public ApiRedisCache(final JedisPool jedisPool, final String redisPrefix, final int maxAgeInSeconds) {
		super(jedisPool, redisPrefix, maxAgeInSeconds);
	}

	@Override
	public JSONObject getJSONObject(final String key) throws CacheException {
		final String jsonString;
		synchronized (this.jedis) {
			jsonString = this.jedis.get(this.jedisKey(key));
		}
		if (jsonString == null) {
			return null;
		} else {
			try {
				return new JSONObject(jsonString);
			} catch (JSONException e) {
				throw new CacheException("Error storing JSON object in API cache", e);
			}
		}
	}

	@Override
	public synchronized void put(final String key, final JSONObject jsonObject) throws CacheException {
		this.jedis.set(this.jedisKey(key), jsonObject.toString());
		this.jedis.expire(this.jedisKey(key), this.maxAgeInSeconds);
	}

}
