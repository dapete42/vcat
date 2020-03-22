package vcat.redis.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import vcat.cache.CacheException;

public class ApiRedisCacheTest {

	private final static String REDIS_PREFIX = "prefix-";

	private Jedis jedisMock;

	private JedisPool jedisPoolMock;

	private ApiRedisCache underTest;

	@BeforeEach
	public void beforeEach() {

		jedisPoolMock = mock(JedisPool.class, Answers.RETURNS_DEEP_STUBS);

		jedisMock = mock(Jedis.class, Answers.RETURNS_DEEP_STUBS);
		when(jedisPoolMock.getResource()).thenReturn(jedisMock);

		underTest = spy(new ApiRedisCache(jedisPoolMock, REDIS_PREFIX, 1000));

	}

	@Test
	public void getJSONObject() throws CacheException {

		final JsonObject jsonIn = Json.createObjectBuilder().add("test1", 1).add("test2", 2).build();

		when(jedisMock.get(eq(REDIS_PREFIX + "abc"))).thenReturn(jsonIn.toString());

		final JsonObject jsonOut = underTest.getJSONObject("abc");

		assertEquals(jsonIn.getInt("test1"), jsonOut.getInt("test1"));
		assertEquals(jsonIn.getInt("test2"), jsonOut.getInt("test2"));

	}

	@Test
	public void getJSONObjectInvalid() throws CacheException {

		when(jedisMock.get(eq(underTest.jedisKey("abc")))).thenReturn("not json");

		final CacheException thrown = assertThrows(CacheException.class, () -> underTest.getJSONObject("abc"));

		assertEquals("Error reading JSON object from API cache, invalid content: 'not json'", thrown.getMessage());

	}

	@Test
	public void getJSONObjectNull() throws CacheException {

		assertNull(underTest.getJSONObject("abc"));

	}

	@Test
	public void put() throws CacheException {

		final Transaction transactionMock = mock(Transaction.class);
		when(jedisMock.multi()).thenReturn(transactionMock);
		final JsonObject json = Json.createObjectBuilder().add("test1", 1).add("test2", 2).build();

		underTest.put("abc", json);

		final String key = underTest.jedisKey("abc");
		verify(jedisMock).multi();
		verify(transactionMock).set(eq(key), eq(json.toString()));
		verify(transactionMock).expire(eq(key), eq(1000));
		verify(transactionMock).exec();

	}

}
