package vcat.cache.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vcat.cache.CacheException;

class ApiFileCacheTest {

	private Path tempDirectory;

	private ApiFileCache underTest;

	@BeforeEach
	void setUp() throws IOException, CacheException {
		tempDirectory = Files.createTempDirectory("ApiFileCacheTest");
		underTest = new ApiFileCache(tempDirectory, 10);
	}

	@AfterEach
	void tearDown() throws IOException {
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
	void testPutAndGetJSONObject() throws CacheException {

		final JsonObject testObject = Json.createObjectBuilder().add("test", 123).build();

		underTest.put("test", testObject);
		final JsonObject resultObject = underTest.getJSONObject("test");

		assertEquals(testObject.getInt("test"), resultObject.getInt("test"));

	}

}
