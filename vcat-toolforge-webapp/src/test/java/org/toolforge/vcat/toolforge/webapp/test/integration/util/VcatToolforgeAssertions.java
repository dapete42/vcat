package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

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

    public static void assertImageEquals(String expectedReferenceImage, byte[] actualImageData) throws IOException {
        final String fileName = expectedReferenceImage + ".png";
        final String resourceName = "/reference-images/" + fileName;
        try (var expectedResourceStream = VcatToolforgeAssertions.class.getResourceAsStream(resourceName);
             var actualImageStream = new ByteArrayInputStream(actualImageData)) {
            if (expectedResourceStream == null) {
                final Path outputFile = Paths.get("target", resourceName);
                Files.createDirectories(outputFile.getParent());
                Files.copy(actualImageStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
                fail("Could not find reference image %s. It has been initialized at %s, but needs to be moved to %s and committed to Git."
                        .formatted(expectedReferenceImage, outputFile, Paths.get("src", "test", "resources", resourceName)));
            } else {
                assertImageEquals(expectedResourceStream, actualImageStream);
            }
        }
    }

    private static void assertImageEquals(InputStream expectedResourceStream, ByteArrayInputStream actualImageStream) throws IOException {
        final var expectedImage = ImageIO.read(expectedResourceStream);
        final var actualImage = ImageIO.read(actualImageStream);
        assertEquals(expectedImage.getHeight(), actualImage.getHeight());
        assertEquals(expectedImage.getWidth(), actualImage.getWidth());
        for (int x = 0; x < expectedImage.getWidth(); x++) {
            for (int y = 0; y < expectedImage.getHeight(); y++) {
                assertEquals(expectedImage.getRGB(x, y), actualImage.getRGB(x, y));
            }
        }
    }

}
