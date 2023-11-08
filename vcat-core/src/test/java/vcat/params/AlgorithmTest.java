package vcat.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the {@link Algorithm} enumeration.
 *
 * @author Peter Schlömer
 */
class AlgorithmTest {

    /**
     * Test for all getter methods of the {@link Algorithm} enumeration.
     */
    @Test
    void testGetters() {
        for (Algorithm algorithm : Algorithm.values()) {
            assertNotNull(algorithm.getProgram());
        }
    }

    @Test
    void testValueOfIgnoreCase() {
        for (Algorithm algorithm : Algorithm.values()) {
            final String name = algorithm.name();
            assertEquals(algorithm, Algorithm.valueOfIgnoreCase(name));
            assertEquals(algorithm, Algorithm.valueOfIgnoreCase(name.toUpperCase()));
            assertEquals(algorithm, Algorithm.valueOfIgnoreCase(name.toLowerCase()));
            assertNull(Algorithm.valueOfIgnoreCase(name + "xxx"));
        }
    }

}
