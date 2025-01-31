package org.toolforge.vcat.toolforge.webapp.test.integration.docker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.toolforge.vcat.params.OutputFormat;
import org.toolforge.vcat.toolforge.webapp.test.integration.docker.util.VcatToolforgeContainers;
import org.toolforge.vcat.toolforge.webapp.test.integration.docker.util.VcatToolforgeDockerITBase;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertContains;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertContentType;

/**
 * Integration tests for the {@code /render} endpoint using a simulated environment with MariaDB and VCat.
 */
class RenderIT extends VcatToolforgeDockerITBase {

    @ParameterizedTest
    @CsvSource(textBlock = """
            render?wiki=dewiki&category=Berlin&format=gv
            render?wiki=dewiki&title=Berlin&format=gv
            """)
    void renderGraphviz(String path) throws InterruptedException, IOException {
        final var response = VcatToolforgeContainers.instance().getHttpResponse(path);

        // returns with status 200
        assertEquals(200, response.statusCode());
        assertContentType(OutputFormat.GraphvizRaw.getMimeType(), response);
        response.body();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            render?wiki=dewiki&category=Berlin,            PNG
            render?wiki=dewiki&category=Berlin&format=GIF, GIF
            render?wiki=dewiki&category=Berlin&format=PDF, PDF
            render?wiki=dewiki&category=Berlin&format=SVG, SVG
            """)
    void renderImage(String path, OutputFormat expectedOutputFormat) throws InterruptedException, IOException {
        final var response = VcatToolforgeContainers.instance().getHttpResponse(path);

        // returns with status 200
        assertEquals(200, response.statusCode());
        assertContentType(expectedOutputFormat.getMimeType(), response);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            render,             Parameter &#39;wiki&#39; missing.
            render?wiki=dewiki, Parameter &#39;title&#39; or &#39;category&#39; missing.
            """)
    void renderMissingParameters(String path, String expectedText) throws InterruptedException, IOException {
        final var response = VcatToolforgeContainers.instance().getHttpResponseStringBody(path);

        // returns a HTML error page
        assertEquals(400, response.statusCode());
        assertEquals("text/html", response.headers().firstValue("Content-Type").orElse(null));
        assertContains(expectedText, response.body());
    }

}
