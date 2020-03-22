package vcat.redis.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class StringRedisCacheTest {

	private final static String REDIS_PREFIX = "prefix-";

	private Jedis jedisMock;

	private JedisPool jedisPoolMock;

	private StringRedisCache underTest;

	@BeforeEach
	public void beforeEach() {

		jedisPoolMock = mock(JedisPool.class, Answers.RETURNS_DEEP_STUBS);

		jedisMock = mock(Jedis.class, Answers.RETURNS_DEEP_STUBS);
		when(jedisPoolMock.getResource()).thenReturn(jedisMock);

		underTest = spy(new StringRedisCache(jedisPoolMock, REDIS_PREFIX, 1000));

	}

	@Test
	public void containsKeyFalse() {

		assertFalse(underTest.containsKey("abc"));

	}

	@Test
	public void containsKeyTrue() {

		when(jedisMock.exists(eq("abc"))).thenReturn(true);
		doReturn("abc").when(underTest).jedisKey(eq("abc"));

		assertTrue(underTest.containsKey("abc"));

	}

	@Test
	public void jedisKey() {

		assertEquals(REDIS_PREFIX + "abc", underTest.jedisKey("abc"));

	}

	@Test
	public void jedisKeyBytes() {

		doReturn("äbc").when(underTest).jedisKey(eq("äbc"));

		assertArrayEquals("äbc".getBytes(StandardCharsets.UTF_8), underTest.jedisKeyBytes("äbc"));

	}

	@Test
	public void purge() {
		
		underTest.purge();
		
		verifyNoInteractions(jedisPoolMock);
		
	}
	
	@Test
	public void remove() {

		when(jedisMock.del(eq("abc"))).thenReturn(123L);
		doReturn("abc").when(underTest).jedisKey(eq("abc"));

		assertEquals(123L, underTest.remove("abc"));

	}

}
