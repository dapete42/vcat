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

    public static void assertImageEquals(String expectedReferenceImage, byte[] actualImageData) throws IOException {
        final String fileName = expectedReferenceImage + ".png";
        final String resourceName = "/reference-images/" + fileName;
        final Path actualImageFile = Paths.get("target", resourceName);
        Files.createDirectories(actualImageFile.getParent());
        try (var actualImageStream = new ByteArrayInputStream(actualImageData)) {
            Files.copy(actualImageStream, actualImageFile, StandardCopyOption.REPLACE_EXISTING);
        }
        try (var expectedResourceStream = VcatToolforgeAssertions.class.getResourceAsStream(resourceName)) {
            if (expectedResourceStream == null) {
                fail("Could not find reference image %s. It has been initialized at %s, but needs to be moved to %s and committed to Git."
                        .formatted(expectedReferenceImage, actualImageFile, Paths.get("src", "test", "resources", resourceName)));
            } else {
                if (!areImagesEqual(expectedResourceStream, actualImageFile)) {
                    fail("Results for reference image %s differ. The actual image is at %s."
                            .formatted(expectedReferenceImage, actualImageFile));
                }
            }
        }
    }

    private static boolean areImagesEqual(InputStream expectedResourceStream, Path actualImageFile) throws IOException {
        final var expectedImage = ImageIO.read(expectedResourceStream);
        final var actualImage = ImageIO.read(actualImageFile.toFile());
        if (expectedImage.getHeight() != actualImage.getHeight() || expectedImage.getWidth() != actualImage.getWidth()) {
            return false;
        }
        for (int x = 0; x < expectedImage.getWidth(); x++) {
            for (int y = 0; y < expectedImage.getHeight(); y++) {
                if (expectedImage.getRGB(x, y) != actualImage.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

}
