package vcat.redis.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import vcat.cache.CacheException;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

public class MetadataRedisCacheTest {

	private class TestWiki implements IWiki {

		private final String apiUrl;

		TestWiki(final String apiUrl) {
			this.apiUrl = apiUrl;
		}

		@Override
		public String getApiUrl() {
			return apiUrl;
		}

		@Override
		public String getDisplayName() {
			return "displayName";
		}

		@Override
		public String getName() {
			return "name";
		}

	}

	private final static String REDIS_PREFIX = "prefix-";

	private Jedis jedisMock;

	private JedisPool jedisPoolMock;

	private MetadataRedisCache underTest;

	@BeforeEach
	public void beforeEach() {

		jedisPoolMock = mock(JedisPool.class, Answers.RETURNS_DEEP_STUBS);

		jedisMock = mock(Jedis.class, Answers.RETURNS_DEEP_STUBS);
		when(jedisPoolMock.getResource()).thenReturn(jedisMock);

		underTest = spy(new MetadataRedisCache(jedisPoolMock, REDIS_PREFIX, 1000));

	}

	@Test
	@Disabled
	public void getMetadata() throws CacheException {

		final Metadata metadataIn = new Metadata("articlepath", "server", Collections.singletonMap(0, "0"),
				Collections.singletonMap("1", 1));
		final IWiki wiki = new TestWiki("apiUrl");
		when(jedisMock.get(eq(underTest.jedisKeyBytes(wiki.getApiUrl()))))
				.thenReturn(SerializationUtils.serialize(metadataIn));

		final Metadata metadataOut = underTest.getMetadata(wiki);

		assertEquals(metadataIn.getArticlepath(), metadataOut.getArticlepath());
		assertEquals(metadataIn.getServer(), metadataOut.getServer());

	}

	@Test
	@Disabled
	public void getMetadataInvalid() throws CacheException {

		final IWiki wiki = new TestWiki("apiUrl");
		when(jedisMock.get(eq(underTest.jedisKeyBytes(wiki.getApiUrl())))).thenReturn(new byte[] { 1, 2, 3 });

		final CacheException thrown = assertThrows(CacheException.class, () -> underTest.getMetadata(wiki));

		assertEquals("Error reading JSON object from API cache, invalid content: 'not json'", thrown.getMessage());

	}

	@Test
	public void getMetadataNull() throws CacheException {

		final IWiki wiki = new TestWiki("apiUrl");

		assertNull(underTest.getMetadata(wiki));

	}

	@Test
	public void put() throws CacheException {

		final Transaction transactionMock = mock(Transaction.class);
		when(jedisMock.multi()).thenReturn(transactionMock);
		final Metadata metadata = new Metadata("articlepath", "server", Collections.singletonMap(0, "0"),
				Collections.singletonMap("1", 1));
		final IWiki wiki = new TestWiki("apiUrl");

		underTest.put(wiki, metadata);

		final byte[] keyBytes = underTest.jedisKeyBytes(wiki.getApiUrl());
		verify(jedisMock).multi();
		verify(transactionMock).set(eq(keyBytes), eq(SerializationUtils.serialize(metadata)));
		verify(transactionMock).expire(eq(keyBytes), eq(1000));
		verify(transactionMock).exec();

	}

}