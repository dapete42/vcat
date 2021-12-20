package vcat.params;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * JUnit tests for the {@link OutputFormat} enumeration.
 * 
 * @author Peter Schlömer
 */
class OutputFormatTest {

	/**
	 * Test for the {@link OutputFormat#valueOfIgnoreCase(String)} method.
	 */
	@Test
	void testValueOfIgnoreCase() {
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
	void testGetters() {
		for (OutputFormat format : OutputFormat.values()) {
			assertNotNull(format.getFileExtension());
			assertNotNull(format.getParameterNames());
			switch (format) {
			case _HTMLGIF:
			case _HTMLPNG:
			case GraphvizRaw:
				assertNull(format.getGraphvizTypeParameter());
				break;
			default:
				assertNotNull(format.getGraphvizTypeParameter());
			}
			switch (format) {
			case _Imagemap:
				assertNull(format.getMimeType());
				break;
			default:
				assertNotNull(format.getMimeType());
			}
			if (format.hasImageMapOutputFormat()) {
				assertNotNull(format.getImageMapOutputFormat());
			} else {
				assertNull(format.getImageMapOutputFormat());
			}
		}
	}

}
