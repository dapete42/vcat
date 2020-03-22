package vcat.cache.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vcat.cache.CacheException;

public class AbstractFileCacheTest {

	private class FileCacheImpl extends AbstractFileCache<String> {

		protected FileCacheImpl(File cacheDirectory, int maxAgeInSeconds) throws CacheException {
			super(cacheDirectory, "AbstractFileCacheTest-FileCacheImpl", "", maxAgeInSeconds);
		}

	}

	private Path tempDirectory;

	private FileCacheImpl underTest;

	@BeforeEach
	public void setUp() throws IOException, CacheException {
		tempDirectory = Files.createTempDirectory("ApiFileCacheTest");
		underTest = new FileCacheImpl(tempDirectory.toFile(), 10);
	}

	@AfterEach
	public void tearDown() throws IOException {
		underTest.clear();
		if (tempDirectory != null) {
			Files.delete(tempDirectory);
		}
	}

	@Test
	public void testClear() throws CacheException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

		underTest.put("test", testBytes);
		underTest.clear();

		byte[] resultBytes = underTest.get("test");

		assertNull(resultBytes);

	}

	@Test
	public void testRemove() throws CacheException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

		underTest.put("test", testBytes);
		underTest.put("test2", testBytes);

		underTest.remove("test");

		assertTrue(underTest.containsKey("test2"));

	}

	@Test
	public void testPurge() throws CacheException, InterruptedException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);
		underTest.maxAgeInSeconds = 1;

		underTest.put("test1", testBytes);
		underTest.put("test2", testBytes);
		underTest.purge();

		assertTrue(underTest.containsKey("test1"));
		assertTrue(underTest.containsKey("test2"));

		Thread.sleep(1500);
		underTest.put("test3", testBytes);
		underTest.purge();

		assertFalse(underTest.containsKey("test1"));
		assertFalse(underTest.containsKey("test2"));
		assertTrue(underTest.containsKey("test3"));

	}

	@Test
	public void testContainsKey() throws CacheException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

		underTest.put("test", testBytes);

		assertTrue(underTest.containsKey("test"));

	}

	@Test
	public void testGetAsInputStream() throws CacheException, IOException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

		underTest.put("test", testBytes);

		try (InputStream input = underTest.getAsInputStream("test")) {
			for (int i = 0; i < testBytes.length; i++) {
				assertEquals(input.read(), testBytes[i]);
			}
			assertEquals(-1, input.read());
		}

	}

	@Test
	public void testGetAsInputStreamNull() throws CacheException, IOException {

		assertNull(underTest.getAsInputStream("test"));

	}

	@Test
	public void testPutAndGet() throws CacheException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

		underTest.put("test", testBytes);
		byte[] resultBytes = underTest.get("test");

		assertArrayEquals(testBytes, resultBytes);

	}

	@Test
	public void testPutFile() throws CacheException, IOException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

		Path tempFile = null;
		try {

			tempFile = Files.createTempFile("AbstractFileCacheTest-testPutFileMove", "");
			try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
				outputStream.write(testBytes);
			}

			underTest.putFile("test", tempFile.toFile(), false);
			byte[] resultBytes = underTest.get("test");

			assertTrue(Files.exists(tempFile));
			assertArrayEquals(testBytes, resultBytes);

		} finally {
			if (tempFile != null) {
				Files.deleteIfExists(tempFile);
			}
		}

	}

	@Test
	public void testPutFileMove() throws CacheException, IOException {

		byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

		Path tempFile = null;
		try {

			tempFile = Files.createTempFile("AbstractFileCacheTest-testPutFileMove", "");
			try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
				outputStream.write(testBytes);
			}

			underTest.putFile("test", tempFile.toFile(), true);
			byte[] resultBytes = underTest.get("test");

			assertFalse(Files.exists(tempFile));
			assertArrayEquals(testBytes, resultBytes);

		} finally {
			if (tempFile != null) {
				Files.deleteIfExists(tempFile);
			}
		}

	}

}
