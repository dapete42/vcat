package org.toolforge.vcat.junit;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.toolforge.vcat.params.AllParams;
import org.toolforge.vcat.params.VCatFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UtilityClass
public class TestUtils {

    public static final Path expectedDirectory = Path.of("src", "test", "resources", "expected");

    static final Path testApiClientCacheDirectory = Path.of("src", "test", "resources", "TestApiClient-cache");

    private static String readGraphvizFileForTesting(final Path graphvizFile) throws IOException {
        return removeCreditLine(Files.readString(graphvizFile, StandardCharsets.UTF_8));
    }

    private static String removeCreditLine(final String graphvizSourceCode) {
        return graphvizSourceCode.replaceAll("// Created by GraphWriter at (.*?)\n", "")
                .replace("\r\n", "\n");
    }

    @SafeVarargs
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

    public static void genericRenderToFileTest(
            TestMode mode, Class<?> testClass, String testName, MultivaluedMap<String, String> requestParams,
            Path tempDirectory) throws Exception {

        final var testApiClient = new TestApiClient();

        final String fileName = testClass.getSimpleName() + '-' + testName + ".gv";
        final Path actualFile = tempDirectory.resolve(fileName);
        final Path expectedFile = TestUtils.expectedDirectory.resolve(fileName);

        if (mode == TestMode.GenerateExpected) {
            testApiClient.setCallRealApi(true);
        }

        AllParams params = new AllParams(requestParams, "", testApiClient);

        final var underTest = new VCatFactory(testApiClient).createInstance(params);

        underTest.renderToFile(actualFile);

        if (mode == TestMode.GenerateExpected) {
            testApiClient.setCallRealApi(false);
            underTest.renderToFile(expectedFile);
        }

        TestUtils.assertGraphvizFilesEquals(expectedFile, actualFile);
    }

    public static void assertGraphvizFilesEquals(Path expectedFile, Path actualFile) throws IOException {
        final String actualString = readGraphvizFileForTesting(actualFile);
        final String expectedString = readGraphvizFileForTesting(expectedFile);
        assertEquals(expectedString, actualString);
    }

    public MultivaluedMap<String, String> requestParamMap(Map<String, List<String>> map) {
        MultivaluedMap<String, String> requestParamMap = new MultivaluedHashMap<>();
        map.forEach(requestParamMap::addAll);
        return requestParamMap;
    }

}
