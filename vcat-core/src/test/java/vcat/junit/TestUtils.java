package vcat.junit;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UtilityClass
public class TestUtils {

    public static final Path expectedDirectory = Path.of("src", "test", "resources", "expected");

    static final Path testApiClientCacheDirectory = Path.of("src", "test", "resources", "TestApiClient-cache");

    public static Map<String, String[]> paramMap(Map<String, String> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new String[]{entry.getValue()}));
    }

    static String readGraphvizFileForTesting(final Path graphvizFile) throws IOException {
        return removeCreditLine(Files.readString(graphvizFile, StandardCharsets.UTF_8));
    }

    static String removeCreditLine(final String graphvizSourceCode) {
        return graphvizSourceCode.replaceAll("// Created by GraphWriter at (.*?)\n", "");
    }

    static void generateExpected(Supplier<CanGenerateExpected>... constructors) throws Exception {
        for (var constructor : constructors) {
            constructor.get().generateExpected();
        }
    }

    static void deleteAndRecreateDirectories(Path... directories) throws IOException {
        for (var directory : directories) {
            if (!directory.startsWith(Path.of("src"))) {
                throw new IllegalArgumentException(("This method must only be called for paths under src/test/resources/ (called for %s)".formatted(directory)));
            }
            FileUtils.deleteDirectory(directory.toFile());
            Files.createDirectories(directory);
        }
    }

    public static void assertGraphvizFilesEquals(Path expectedFile, Path actualFile) throws IOException {
        final String actualString = readGraphvizFileForTesting(actualFile);
        final String expectedString = readGraphvizFileForTesting(expectedFile);
        assertEquals(expectedString, actualString);
    }

}
