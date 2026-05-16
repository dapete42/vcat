package org.toolforge.vcat.toolforge.webapp.test;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class FontRenderingUtils {

    private FontRenderingUtils() {
    }

    public static void checkImageEquals(String expectedReferenceImage, byte[] actualImageData) throws IOException {
        final String fileName = expectedReferenceImage + ".png";
        final String resourceName = "/reference-images/" + fileName;
        final Path actualImageFile = Paths.get("target", resourceName);

        try (var expectedResourceStream = TestFontRendering.class.getResourceAsStream(resourceName)) {
            if (expectedResourceStream == null) {
                writeImageFile(actualImageData, actualImageFile);
                throw new RuntimeException("Could not find reference image %s. It has been initialized at %s, but needs to be moved to %s and committed to Git."
                        .formatted(expectedReferenceImage, actualImageFile, Paths.get("src", "test", "resources", resourceName)));
            }
            if (!areImagesEqual(expectedResourceStream, actualImageData)) {
                writeImageFile(actualImageData, actualImageFile);
                throw new RuntimeException("Results for reference image %s differ. The actual image is at %s."
                        .formatted(expectedReferenceImage, actualImageFile));
            }
        }
    }

    private static void writeImageFile(byte[] actualImageData, Path actualImageFile) throws IOException {
        Files.createDirectories(actualImageFile.getParent());
        try (var actualImageStream = new ByteArrayInputStream(actualImageData)) {
            Files.copy(actualImageStream, actualImageFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean areImagesEqual(InputStream expectedResourceStream, byte[] actualImageData) throws IOException {
        final var expectedImage = ImageIO.read(expectedResourceStream);
        final BufferedImage actualImage;
        try (var actualImageStream = new ByteArrayInputStream(actualImageData)) {
            actualImage = ImageIO.read(actualImageStream);
        }

        final var imageComparison = new ImageComparison(expectedImage, actualImage);
        final var imageComparisonResult = imageComparison.compareImages();
        return imageComparisonResult.getImageComparisonState() == ImageComparisonState.MATCH;
    }

}
