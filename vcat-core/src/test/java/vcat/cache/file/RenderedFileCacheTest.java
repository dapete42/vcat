package vcat.cache.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vcat.cache.CacheException;
import vcat.params.CombinedParams;
import vcat.params.GraphvizParams;
import vcat.params.OutputFormat;
import vcat.params.VCatParams;
import vcat.test.TestWiki;

public class RenderedFileCacheTest {

	private Path tempDirectory;

	private RenderedFileCache<TestWiki> underTest;

	@BeforeEach
	public void setUp() throws IOException, CacheException {
		tempDirectory = Files.createTempDirectory("ApiFileCacheTest");
		underTest = new RenderedFileCache<>(tempDirectory.toFile(), 10);
	}

	@AfterEach
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
