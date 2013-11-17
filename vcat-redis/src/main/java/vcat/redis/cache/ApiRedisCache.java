package vcat.redis.cache;

import org.json.JSONException;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.redis.Messages;

public class ApiRedisCache extends StringRedisCache implements IApiCache {

	public ApiRedisCache(final JedisPool jedisPool, final String redisPrefix, final int maxAgeInSeconds) {
		super(jedisPool, redisPrefix, maxAgeInSeconds);
	}

	@Override
	public JSONObject getJSONObject(final String key) throws CacheException {
		final String jsonString;
		final Jedis jedis = this.jedisPool.getResource();
		this.jedisPool.returnResource(jedis);
		jsonString = jedis.get(this.jedisKey(key));
		if (jsonString == null) {
			return null;
		} else {
			try {
				return new JSONObject(jsonString);
			} catch (JSONException e) {
				throw new CacheException(Messages.getString("ApiRedisCache.Exception.StoreJSON"), e);
			}
		}
	}

	@Override
	public synchronized void put(final String key, final JSONObject jsonObject) throws CacheException {
		final String jedisKey = this.jedisKey(key);
		final Jedis jedis = this.jedisPool.getResource();
		Transaction t = jedis.multi();
		t.set(jedisKey, jsonObject.toString());
		t.expire(jedisKey, this.maxAgeInSeconds);
		t.exec();
		this.jedisPool.returnResource(jedis);
	}

}
