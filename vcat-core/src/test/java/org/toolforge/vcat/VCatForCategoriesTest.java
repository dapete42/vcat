package org.toolforge.vcat;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.toolforge.vcat.junit.CanGenerateExpected;
import org.toolforge.vcat.junit.TestMode;
import org.toolforge.vcat.junit.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class VCatForCategoriesTest implements CanGenerateExpected {

    private Path tempDirectory;

    @BeforeEach
    void beforeEach() throws IOException {
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
        renderToFileDepth(TestMode.GenerateExpected);
        renderToFileMultipleCategories(TestMode.GenerateExpected);
        afterEach();
    }

    private void genericRenderToFileTest(TestMode mode, String testName, MultivaluedMap<String, String> requestParams) throws Exception {
        TestUtils.genericRenderToFileTest(mode, getClass(), testName, requestParams, tempDirectory);
    }

    @Test
    void renderToFile() throws Exception {
        renderToFile(TestMode.Test);
    }

    private void renderToFile(TestMode mode) throws Exception {
        genericRenderToFileTest(mode, "renderToFile", TestUtils.requestParamMap(Map.of(
                "wiki", List.of("de.wikipedia.org"),
                "category", List.of("Deutschland"),
                "format", List.of("gv"),
                "links", List.of("wiki")
        )));
    }

    @Test
    void renderToFileDepth() throws Exception {
        renderToFileDepth(TestMode.Test);
    }

    private void renderToFileDepth(TestMode mode) throws Exception {
        genericRenderToFileTest(mode, "renderToFileDepth", TestUtils.requestParamMap(Map.of(
                "wiki", List.of("de.wikipedia.org"),
                "category", List.of("Deutschland"),
                "format", List.of("gv"),
                "links", List.of("wiki"),
                "depth", List.of("4")
        )));
    }

    @Test
    void renderToFileMultipleCategories() throws Exception {
        renderToFileMultipleCategories(TestMode.Test);
    }

    private void renderToFileMultipleCategories(TestMode mode) throws Exception {
        genericRenderToFileTest(mode, "renderToFileMultipleCategories", TestUtils.requestParamMap(Map.of(
                "wiki", List.of("de.wikipedia.org"),
                "category", List.of("Deutschland", "Belgien"),
                "format", List.of("gv"),
                "links", List.of("wiki"),
                "depth", List.of("4")
        )));
    }

}