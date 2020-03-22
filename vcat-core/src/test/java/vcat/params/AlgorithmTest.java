package vcat.params;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import vcat.params.Algorithm;

/**
 * JUnit tests for the {@link Algorithm} enumeration.
 * 
 * @author Peter Schlömer
 */
public class AlgorithmTest {

	/**
	 * Test for all getter methods of the {@link Algorithm} enumeration.
	 */
	@Test
	public void testGetters() {
		for (Algorithm algorithm : Algorithm.values()) {
			algorithm.getProgram();
		}
	}

	@Test
	public void testValueOfIgnoreCase() {
		for (Algorithm algorithm : Algorithm.values()) {
			final String name = algorithm.name();
			assertEquals(algorithm, Algorithm.valueOfIgnoreCase(name));
			assertEquals(algorithm, Algorithm.valueOfIgnoreCase(name.toUpperCase()));
			assertEquals(algorithm, Algorithm.valueOfIgnoreCase(name.toLowerCase()));
			assertNull(Algorithm.valueOfIgnoreCase(name + "xxx"));
		}
	}

}
