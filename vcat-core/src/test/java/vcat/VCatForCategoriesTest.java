package vcat;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vcat.cache.CacheException;
import vcat.graphviz.GraphvizException;
import vcat.junit.CanGenerateExpected;
import vcat.junit.TestApiClient;
import vcat.junit.TestMode;
import vcat.junit.TestUtils;
import vcat.mediawiki.interfaces.CategoryProvider;
import vcat.mediawiki.interfaces.Wiki;
import vcat.params.AllParams;
import vcat.params.VCatFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VCatForCategoriesTest implements CanGenerateExpected {

    private TestApiClient testApiClient;

    private Path tempDirectory;

    @BeforeEach
    void beforeEach() throws CacheException, IOException {
        testApiClient = new TestApiClient();
        tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
    }

    @AfterEach
    void afterEach() throws IOException {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    @Override
    public void generateExpected() throws Exception {
        beforeEach();
        renderToFile(TestMode.GenerateExpected);
        afterEach();
    }

    @Test
    void renderToFile() throws GraphvizException, IOException, VCatException {
        renderToFile(TestMode.Test);
    }

    private void renderToFile(TestMode mode) throws GraphvizException, IOException, VCatException {

        final String fileName = getClass().getSimpleName() + "-renderToFile.gv";
        final Path actualFile = tempDirectory.resolve(fileName);
        final Path expectedFile = TestUtils.expectedDirectory.resolve(fileName);

        if (mode == TestMode.GenerateExpected) {
            testApiClient.setCallRealApi(true);
        }

        AllParams params = new AllParams(TestUtils.paramMap(Map.of(
                "wiki", "de.wikipedia.org",
                "category", "Deutschland",
                "format", "gv",
                "links", "wiki",
                "depth", "4"
        )), "", testApiClient);

        final var underTest = new VCatFactory(testApiClient).createInstance(params);
        assertEquals(VCatForCategories.class, underTest.getClass());

        underTest.renderToFile(actualFile);

        if (mode == TestMode.GenerateExpected) {
            testApiClient.setCallRealApi(false);
            underTest.renderToFile(expectedFile);
        }

        TestUtils.assertGraphvizFilesEquals(expectedFile, actualFile);

    }

}