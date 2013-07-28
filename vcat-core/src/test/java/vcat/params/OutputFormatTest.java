package vcat.params;

import static org.junit.Assert.*;

import org.junit.Test;

import vcat.params.OutputFormat;

/**
 * JUnit tests for the {@link OutputFromat} enumeration.
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
			// Get all enumeration values with upper/lower case of their name()
			assertEquals(format, OutputFormat.valueOfIgnoreCase(format.name().toLowerCase()));
			assertEquals(format, OutputFormat.valueOfIgnoreCase(format.name().toUpperCase()));
			// Check invalid values are null
			assertNull(OutputFormat.valueOfIgnoreCase(format.name() + "xxxxxxxx"));
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
			format.getMimeType();
		}
	}

}
