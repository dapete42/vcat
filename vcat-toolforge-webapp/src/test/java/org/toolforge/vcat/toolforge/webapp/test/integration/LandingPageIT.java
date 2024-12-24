package org.toolforge.vcat.toolforge.webapp.test.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeContainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.toolforge.vcat.toolforge.webapp.test.integration.util.VcatToolforgeAssertions.assertContains;

/**
 * Integration tests for the landing page using a simulated environment with MariaDB and VCat.
 */
class LandingPageIT {

    @BeforeAll
    static void beforeAll() {
        VcatToolforgeContainers.instance().start();
    }

    @Test
    void landingPage() throws InterruptedException, IOException {
        final var response = VcatToolforgeContainers.instance().getHttpResponseStringBody("");

        // returns an HTML page with status 200
        assertEquals(200, response.statusCode());
        assertEquals("text/html", response.headers().firstValue("Content-Type").orElse(null));
        assertContains("<h1>vCat</h1>", response.body());
    }

}
