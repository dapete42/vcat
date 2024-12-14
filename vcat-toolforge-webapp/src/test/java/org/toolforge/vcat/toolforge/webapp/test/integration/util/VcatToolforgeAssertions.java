package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public abstract class VcatToolforgeAssertions {

    private VcatToolforgeAssertions() {
    }

    public static void assertContains(String expected, String actual) {
        assertTrue(actual.contains(expected), "'" + expected + "' not found in '" + actual + "'");
    }

    public static void assertContentType(String expected, HttpResponse<?> actualResponse) {
        actualResponse.headers().firstValue("Content-Type").ifPresentOrElse(
                actualContentType -> assertTrue(actualContentType.startsWith(expected),
                        "'" + actualContentType + "' does not start with '" + expected + "'"),
                () -> fail("Content-Type missing, should be '" + expected + "'")
        );
    }

}
