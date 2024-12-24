package org.toolforge.vcat.toolforge.webapp.test.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.toolforge.vcat.params.OutputFormat;
import org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeContainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertContentType;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertImageEquals;

/**
 * Integration tests for the {@code /render} endpoint using a simulated environment with MariaDB and VCat.
 */
class FontRenderingIT {

    @BeforeAll
    static void beforeAll() {
        VcatToolforgeContainers.instance().start();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            äöüÄÖÜßxxxxx, lang-de
            ベルリンxxxxx, lang-ja
            新德國臺灣xxxxx, lang-zh
            """)
    void testFontRendering(String title, String expectedReferenceImage) throws InterruptedException, IOException {
        final var response = VcatToolforgeContainers.instance().getHttpResponse("render?wiki=enwiki&title=%s".formatted(title));

        // returns with status 200
        assertEquals(200, response.statusCode());
        assertContentType(OutputFormat.PNG.getMimeType(), response);
        assertImageEquals(expectedReferenceImage, response.body());
    }

}
