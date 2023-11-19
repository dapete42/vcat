package org.toolforge.vcat.cache.file;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.toolforge.vcat.cache.CacheException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AbstractFileCacheTest {

    private static class FileCacheImpl extends AbstractFileCache<String> {

        @Serial
        private static final long serialVersionUID = 962489659800542366L;

        protected FileCacheImpl(Path cacheDirectory, int maxAgeInSeconds) throws CacheException {
            super(cacheDirectory, "AbstractFileCacheTest-FileCacheImpl", "", maxAgeInSeconds);
        }

    }

    private Path tempDirectory;

    private FileCacheImpl underTest;

    @BeforeEach
    public void setUp() throws IOException, CacheException {
        tempDirectory = Files.createTempDirectory("ApiFileCacheTest");
        underTest = new FileCacheImpl(tempDirectory, 10);
    }

    @AfterEach
    public void tearDown() throws IOException {
        try {
            underTest.clear();
        } catch (CacheException e) {
            // ignore
        }
        if (tempDirectory != null) {
            PathUtils.deleteDirectory(tempDirectory);
        }
    }

    @Test
    void testClear() throws CacheException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

        underTest.put("test", testBytes);
        underTest.clear();

        byte[] resultBytes = underTest.get("test");

        assertNull(resultBytes);

    }

    @Test
    void testRemove() throws CacheException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

        underTest.put("test", testBytes);
        underTest.put("test2", testBytes);

        underTest.remove("test");

        assertTrue(underTest.containsKey("test2"));

    }

    @Test
    void testPurge() throws CacheException, InterruptedException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);
        underTest.maxAgeInSeconds = 1;

        underTest.put("test1", testBytes);
        underTest.put("test2", testBytes);
        underTest.purge();

        assertTrue(underTest.containsKey("test1"));
        assertTrue(underTest.containsKey("test2"));

        TimeUnit.MILLISECONDS.sleep(1500);
        underTest.put("test3", testBytes);
        underTest.purge();

        assertFalse(underTest.containsKey("test1"));
        assertFalse(underTest.containsKey("test2"));
        assertTrue(underTest.containsKey("test3"));

    }

    @Test
    void testContainsKey() throws CacheException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

        underTest.put("test", testBytes);

        assertTrue(underTest.containsKey("test"));

    }

    @Test
    void testGetAsInputStream() throws CacheException, IOException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

        underTest.put("test", testBytes);

        try (InputStream input = underTest.getAsInputStream("test")) {
            for (byte testByte : testBytes) {
                assertEquals(input.read(), testByte);
            }
            assertEquals(-1, input.read());
        }

    }

    @Test
    void testGetAsInputStreamNull() throws CacheException {

        assertNull(underTest.getAsInputStream("test"));

    }

    @Test
    void testPutAndGet() throws CacheException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

        underTest.put("test", testBytes);
        byte[] resultBytes = underTest.get("test");

        assertArrayEquals(testBytes, resultBytes);

    }

    @Test
    void testPutFile() throws CacheException, IOException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

        Path tempFile = null;
        try {

            tempFile = Files.createTempFile("AbstractFileCacheTest-testPutFileMove", "");
            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                outputStream.write(testBytes);
            }

            underTest.putFile("test", tempFile, false);
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
    void testPutFileMove() throws CacheException, IOException {

        byte[] testBytes = "test".getBytes(StandardCharsets.US_ASCII);

        Path tempFile = null;
        try {

            tempFile = Files.createTempFile("AbstractFileCacheTest-testPutFileMove", "");
            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                outputStream.write(testBytes);
            }

            underTest.putFile("test", tempFile, true);
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
