package org.toolforge.vcat.cache.file;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.mediawiki.Metadata;
import org.toolforge.vcat.test.TestWiki;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MetadataFileCacheTest {

    private Path tempDirectory;

    private MetadataFileCache underTest;

    @BeforeEach
    void setUp() throws IOException, CacheException {
        tempDirectory = Files.createTempDirectory("ApiFileCacheTest");
        underTest = new MetadataFileCache(tempDirectory, 10);
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            underTest.clear();
        } catch (CacheException e) {
            // ignore
        }
        PathUtils.deleteDirectory(tempDirectory);
    }

    @Test
    void testGetMetadataInvalidData() throws CacheException {

        TestWiki wiki = new TestWiki();

        underTest.put(wiki.getApiUrl(), new byte[0]);

        CacheException thrown = assertThrows(CacheException.class, () -> underTest.getMetadata(wiki));

        assertEquals("Error while deserializing cached file to Metadata; removing from cache", thrown.getMessage());

    }

    @Test
    void testGetMetadataNull() throws CacheException {

        TestWiki wiki = new TestWiki();

        assertNull(underTest.getMetadata(wiki));

    }

    @Test
    void testGetMetadataWrongType() throws CacheException {

        TestWiki wiki = new TestWiki();

        underTest.put(wiki.getApiUrl(), SerializationUtils.serialize("Test String"));

        CacheException thrown = assertThrows(CacheException.class, () -> underTest.getMetadata(wiki));

        assertEquals("Error while deserializing cached file to Metadata; removing from cache", thrown.getMessage());

    }

    @Test
    void testPutAndGetMetadata() throws CacheException {

        Metadata testMetadata = new Metadata("articlepath", "server", Collections.singletonMap(1, "test"),
                Collections.singletonMap("test", 1));

        TestWiki wiki = new TestWiki();

        underTest.put(wiki, testMetadata);
        Metadata resultMetadata = underTest.getMetadata(wiki);

        assertEquals(testMetadata.getArticlepath(), resultMetadata.getArticlepath());
        assertEquals(testMetadata.getServer(), resultMetadata.getServer());
        assertEquals(testMetadata.getAllNames(1), resultMetadata.getAllNames(1));
        assertEquals(testMetadata.getAllNamespacesInverse().get("test"),
                resultMetadata.getAllNamespacesInverse().get("test"));

    }

}
