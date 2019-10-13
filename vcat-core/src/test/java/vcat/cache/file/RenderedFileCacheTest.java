package vcat.cache.file;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import vcat.cache.CacheException;
import vcat.mediawiki.IWiki;
import vcat.params.CombinedParams;
import vcat.params.GraphvizParams;
import vcat.params.OutputFormat;
import vcat.params.VCatParams;

public class RenderedFileCacheTest {

	private class TestWiki implements IWiki {

		private static final long serialVersionUID = -508773764595362723L;

		@Override
		public String getApiUrl() {
			return "http://api.url";
		}

		@Override
		public String getDisplayName() {
			return "Test";
		}

		@Override
		public String getName() {
			return "test";
		}

	}

	private Path tempDirectory;

	private RenderedFileCache<TestWiki> underTest;

	@Before
	public void setUp() throws IOException, CacheException {
		tempDirectory = Files.createTempDirectory("ApiFileCacheTest");
		underTest = new RenderedFileCache<>(tempDirectory.toFile(), 10);
	}

	@After
	public void tearDown() throws IOException {
		underTest.clear();
		if (tempDirectory != null) {
			Files.delete(tempDirectory);
		}
	}

	@Test
	public void testgetCacheFilename() throws CacheException {

		VCatParams<TestWiki> vCatParams = new VCatParams<>();
		GraphvizParams graphvizParams = new GraphvizParams();
		graphvizParams.setOutputFormat(OutputFormat.PNG);
		CombinedParams<TestWiki> key = new CombinedParams<>(vCatParams, graphvizParams);

		final String result = underTest.getCacheFilename(key);

		assertTrue(result.endsWith('.' + OutputFormat.PNG.getFileExtension()));

	}

}
