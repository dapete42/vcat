package vcat.redis.cache;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.helpers.MessageFormatter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.redis.Messages;

public class ApiRedisCache extends StringRedisCache implements IApiCache {

	private static final long serialVersionUID = -1779929219116057263L;

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
				try (StringReader stringReader = new StringReader(jsonString);
						JsonReader jsonReader = Json.createReader(stringReader)) {
					return jsonReader.readObject();
				}
			} catch (JsonException e) {
				throw new CacheException(MessageFormatter
						.format(Messages.getString("ApiRedisCache.Exception.ReadJSON"), jsonString).getMessage(), e);
			}
		}
	}

	@Override
	public synchronized void put(final String key, final JsonObject jsonObject) throws CacheException {
		final String jedisKey = this.jedisKey(key);
		transaction(t -> {
			t.set(jedisKey, jsonObject.toString());
			t.expire(jedisKey, this.maxAgeInSeconds);
		});
	}

}
