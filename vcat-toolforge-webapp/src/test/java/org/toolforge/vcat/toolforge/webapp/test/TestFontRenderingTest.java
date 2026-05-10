package org.toolforge.vcat.toolforge.webapp.test;

import jakarta.inject.Inject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.toolforge.vcat.graphviz.GraphvizException;
import org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeITBase;

import java.io.IOException;

import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertImageEquals;

/**
 * Tests for font rendering - only to be run in the Toolforge build environment.
 */
class TestFontRenderingTest extends VcatToolforgeITBase {

    @Inject
    TestFontRendering testFontRendering;

    @ParameterizedTest
    @CsvFileSource(resources = "/testFontRendering.csv")
    void testFontRendering(String text, String expectedReferenceImage) throws GraphvizException, IOException {
        final byte[] responseBytes = testFontRendering.test(text);

        assertImageEquals(expectedReferenceImage, responseBytes);
    }

}
