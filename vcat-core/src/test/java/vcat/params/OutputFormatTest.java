package vcat.params;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import vcat.params.OutputFormat;

/**
 * JUnit tests for the {@link OutputFormat} enumeration.
 * 
 * @author Peter Schlömer
 */
public class OutputFormatTest {

	/**
	 * Test for the {@link OutputFormat#valueOfIgnoreCase(String)} method.
	 */
	@Test
	public void testValueOfIgnoreCase() {
		for (OutputFormat format : OutputFormat.values()) {
			for (String name : format.getParameterNames()) {
				// Get all parameter names for all values with upper/lower case
				assertEquals(format, OutputFormat.valueOfIgnoreCase(name));
				assertEquals(format, OutputFormat.valueOfIgnoreCase(name.toLowerCase()));
				assertEquals(format, OutputFormat.valueOfIgnoreCase(name.toUpperCase()));
				// Check invalid values are null
				assertNull(OutputFormat.valueOfIgnoreCase(name + "xxxxxxxx"));
			}
		}
	}

	/**
	 * Test for all getter methods of the {@link OutputFormat} enumeration.
	 */
	@Test
	public void testGetters() {
		for (OutputFormat format : OutputFormat.values()) {
			format.getFileExtension();
			format.getGraphvizTypeParameter();
			format.getImageMapOutputFormat();
			format.getMimeType();
			format.getParameterNames();
			format.hasImageMapOutputFormat();
		}
	}

}
