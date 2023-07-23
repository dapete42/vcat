package vcat.redis.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import vcat.cache.CacheException;

class ApiRedisCacheTest {

	private final static String REDIS_PREFIX = "prefix-";

	private Jedis jedisMock;

	private JedisPool jedisPoolMock;

	private ApiRedisCache underTest;

	@BeforeEach
	void beforeEach() {

		jedisPoolMock = mock(JedisPool.class, Answers.RETURNS_DEEP_STUBS);

		jedisMock = mock(Jedis.class, Answers.RETURNS_DEEP_STUBS);
		when(jedisPoolMock.getResource()).thenReturn(jedisMock);

		underTest = spy(new ApiRedisCache(jedisPoolMock, REDIS_PREFIX, 1000));

	}

	@Test
	void getJSONObject() throws CacheException {

		final JsonObject jsonIn = Json.createObjectBuilder().add("test1", 1).add("test2", 2).build();

		when(jedisMock.get(eq(REDIS_PREFIX + "abc"))).thenReturn(jsonIn.toString());

		final JsonObject jsonOut = underTest.getJSONObject("abc");

		assertEquals(jsonIn.getInt("test1"), jsonOut.getInt("test1"));
		assertEquals(jsonIn.getInt("test2"), jsonOut.getInt("test2"));

	}

	@Test
	void getJSONObjectInvalid() throws CacheException {

		when(jedisMock.get(eq(underTest.jedisKey("abc")))).thenReturn("not json");

		final CacheException thrown = assertThrows(CacheException.class, () -> underTest.getJSONObject("abc"));

		assertEquals("Error reading JSON object from API cache, invalid content: 'not json'", thrown.getMessage());

	}

	@Test
	void getJSONObjectNull() throws CacheException {

		assertNull(underTest.getJSONObject("abc"));

	}

	@Test
	void put() throws CacheException {

		final Connection connectionMock = mock(Connection.class);
		final Transaction transactionSpy = spy(new Transaction(connectionMock, true));
		doReturn(transactionSpy).when(underTest).newTransaction(any(Connection.class));
		final JsonObject json = Json.createObjectBuilder().add("test1", 1).add("test2", 2).build();

		underTest.put("abc", json);

		final String key = underTest.jedisKey("abc");
		verify(transactionSpy).set(eq(key), eq(json.toString()));
		verify(transactionSpy).expire(eq(key), eq(1000L));

	}

}
