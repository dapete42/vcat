package vcat.cache.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vcat.cache.CacheException;

public class ApiFileCacheTest {

	private Path tempDirectory;

	private ApiFileCache underTest;

	@BeforeEach
	public void setUp() throws IOException, CacheException {
		tempDirectory = Files.createTempDirectory("ApiFileCacheTest");
		underTest = new ApiFileCache(tempDirectory.toFile(), 10);
	}

	@AfterEach
	public void tearDown() throws IOException {
		underTest.clear();
		if (tempDirectory != null) {
			Files.delete(tempDirectory);
		}
	}

	@Test
	public void testPutAndGetJSONObject() throws CacheException {

		final JsonObject testObject = Json.createObjectBuilder().add("test", 123).build();

		underTest.put("test", testObject);
		final JsonObject resultObject = underTest.getJSONObject("test");

		assertEquals(testObject.getInt("test"), resultObject.getInt("test"));

	}

}
