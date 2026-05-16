package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.toolforge.webapp.test.FontRenderingUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    public static void verifyImageEquals(String expectedReferenceImage, byte[] actualImageData) throws IOException {
        try {
            FontRenderingUtils.checkImageEquals(expectedReferenceImage, actualImageData);
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

}
