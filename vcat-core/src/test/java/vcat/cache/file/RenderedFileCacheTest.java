package vcat.cache.file;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import vcat.cache.CacheException;
import vcat.params.CombinedParams;
import vcat.params.GraphvizParams;
import vcat.params.OutputFormat;
import vcat.params.VCatParams;
import vcat.test.TestWiki;

public class RenderedFileCacheTest {

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
