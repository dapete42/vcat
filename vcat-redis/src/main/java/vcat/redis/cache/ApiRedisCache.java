package vcat.redis.cache;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;

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
	public JsonObject getJSONObject(final String key) throws CacheException {
		final String jsonString;
		try (Jedis jedis = this.jedisPool.getResource()) {
			jsonString = jedis.get(this.jedisKey(key));
		}
		if (jsonString == null) {
			return null;
		} else {
			try {
				return Json.createReader(new StringReader(jsonString)).readObject();
			} catch (JsonException e) {
				throw new CacheException(
						String.format(Messages.getString("ApiRedisCache.Exception.ReadJSON"), jsonString), e);
			}
		}
	}

	@Override
	public synchronized void put(final String key, final JsonObject jsonObject) throws CacheException {
		final String jedisKey = this.jedisKey(key);
		try (Jedis jedis = this.jedisPool.getResource()) {
			Transaction t = jedis.multi();
			t.set(jedisKey, jsonObject.toString());
			t.expire(jedisKey, this.maxAgeInSeconds);
			t.exec();
		}
	}

}
