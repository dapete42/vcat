package org.toolforge.vcat.toolforge.webapp.test.integration.docker;

import org.junit.jupiter.api.Test;
import org.toolforge.vcat.toolforge.webapp.test.integration.docker.util.VcatToolforgeContainers;
import org.toolforge.vcat.toolforge.webapp.test.integration.docker.util.VcatToolforgeDockerITBase;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertContains;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertContentType;

/**
 * Integration tests for the landing page using a simulated environment with MariaDB and VCat.
 */
class LandingPageIT extends VcatToolforgeDockerITBase {

    @Test
    void landingPage() throws InterruptedException, IOException {
        final var response = VcatToolforgeContainers.instance().getHttpResponseStringBody("");

        // returns an HTML page with status 200
        assertEquals(200, response.statusCode());
        assertContentType("text/html", response);
        assertContains("<h1>vCat</h1>", response.body());
    }

}
