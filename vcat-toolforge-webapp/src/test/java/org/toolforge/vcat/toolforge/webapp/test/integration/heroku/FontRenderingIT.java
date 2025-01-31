package org.toolforge.vcat.toolforge.webapp.test.integration.heroku;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.toolforge.vcat.graphviz.GraphvizException;
import org.toolforge.vcat.graphviz.GraphvizExternal;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.toolforge.webapp.util.TestFontRenderingHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertImageEquals;

/**
 * Integration tests
 */
class FontRenderingIT {

    private static Graphviz GRAPHVIZ;

    @BeforeAll
    static void beforeAll() {
        // path can be hard coded, this tests only runs in a fixed environment
        GRAPHVIZ = new GraphvizExternal(Paths.get("/layers/fagiani_apt/apt/usr/bin"));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/testFontRendering.csv")
    void testFontRendering(String text, String expectedReferenceImage) throws GraphvizException, IOException {
        Path imageFile = null;
        try {
            final var renderResult = TestFontRenderingHelper.renderImage(GRAPHVIZ, text);
            imageFile = renderResult.imageFile();

            assertImageEquals(expectedReferenceImage, imageFile);
        } finally {
            TestFontRenderingHelper.deleteTempFile(imageFile);
        }
    }

}
