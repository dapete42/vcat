package org.toolforge.vcat.toolforge.webapp.test.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class RenderIT extends AbstractIT {

    private static HttpResponse<String> getHttpResponse(String path) throws IOException, InterruptedException {
        try (var httpClient = HttpClient.newHttpClient()) {
            final var uri = URI.create(VCAT_TOOLFORGE_WEBAPP.getUrl(path));
            final var request = HttpRequest.newBuilder(uri).build();
            final var bodyHandler = HttpResponse.BodyHandlers.ofString();
            return httpClient.send(request, bodyHandler);
        }
    }

    @Test
    void landingPage() throws InterruptedException, IOException {
        final var response = getHttpResponse("");

        // returns a HTML page with status 200
        assertEquals(200, response.statusCode());
        assertEquals("text/html", response.headers().firstValue("Content-Type").orElse(null));
        assertTrue(response.body().contains("<h1>vCat</h1>"));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            render?wiki=dewiki&category=Berlin, image/png
            render?wiki=dewiki&category=Berlin&format=gv, text/plain; charset=UTF-8
            render?wiki=dewiki&title=Berlin&format=gv, text/plain; charset=UTF-8
            """)
    void render(String path, String expectedContentType) throws InterruptedException, IOException {
        final var response = getHttpResponse(path);

        // returns with status 200
        assertEquals(200, response.statusCode());
        response.headers().firstValue("Content-Type").ifPresentOrElse(
                actualContentType -> assertTrue(actualContentType.startsWith(expectedContentType),
                        "'" + actualContentType + "' does not start with '" + expectedContentType + "'"),
                () -> fail("Content-Type missing, should be '" + expectedContentType + "'")
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            render, Parameter &#39;wiki&#39; missing.
            render?wiki=dewiki, Parameter &#39;title&#39; or &#39;category&#39; missing.
            """)
    void renderMissingParameters(String path, String expectedText) throws InterruptedException, IOException {
        final var response = getHttpResponse(path);

        // returns a HTML error page
        assertEquals(400, response.statusCode());
        assertEquals("text/html", response.headers().firstValue("Content-Type").orElse(null));
        assertTrue(response.body().contains(expectedText), "'" + expectedText + "' not found in '" + response.body() + "'");
    }

}
