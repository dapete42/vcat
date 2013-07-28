package vcat.params;

import org.junit.Test;

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

}
