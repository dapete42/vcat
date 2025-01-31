package org.toolforge.vcat.toolforge.webapp.test.integration.docker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.toolforge.vcat.params.OutputFormat;
import org.toolforge.vcat.toolforge.webapp.test.integration.docker.util.VcatToolforgeContainers;
import org.toolforge.vcat.toolforge.webapp.test.integration.docker.util.VcatToolforgeDockerITBase;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertContentType;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertImageEquals;

/**
 * Integration tests for the {@code /render} endpoint using a simulated environment with MariaDB and VCat.
 */
class FontRenderingIT extends VcatToolforgeDockerITBase {

    @ParameterizedTest
    @CsvFileSource(resources = "/testFontRendering.csv")
    void testFontRendering(String text, String expectedReferenceImage) throws InterruptedException, IOException {
        final var response = VcatToolforgeContainers.instance()
                .getHttpResponse("test-font-rendering?text=%s".formatted(URLEncoder.encode(text.replace(' ', '_'), StandardCharsets.UTF_8)));

        // returns with status 200
        assertEquals(200, response.statusCode());
        assertContentType(OutputFormat.PNG.getMimeType(), response);
        assertImageEquals(expectedReferenceImage, response.body());
    }

}
