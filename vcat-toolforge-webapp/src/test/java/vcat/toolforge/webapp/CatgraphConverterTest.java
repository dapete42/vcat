package vcat.toolforge.webapp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import vcat.params.AbstractAllParams;
import vcat.params.OutputFormat;

/**
 * JUnit tests for the {@link CatgraphConverter} class.
 * 
 * @author Peter Schlömer
 */
public class CatgraphConverterTest {

	private static final String[] FALSE_VALUES = { null, "", "0", "false" };

	private static final String[] TRUE_VALUES = { "a", "b", "c", "1", "true", "wahr" };

	private static final String[] WIKI_LIST_LANGUAGE = { "wikibooks", "wikinews", "wikiquote", "wikiversity",
			"wiktionary" };

	private static final String[] WIKI_LIST_NOLANGUAGE = { "commons", "meta" };

	private static void assertSingleValue(final String expectedValue, final Map<String, String[]> map,
			final String key) {
		assertNotNull(map);
		assertTrue(map.containsKey(key));
		assertEquals(1, map.get(key).length);
		assertEquals(expectedValue, map.get(key)[0]);
	}

	private static Map<String, String[]> convertMap(final String... keyValuePairs) {
		final HashMap<String, String[]> map = new HashMap<>();
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final String key = keyValuePairs[i];
			final String value = keyValuePairs[i + 1];
			final String[] values;
			if (map.containsKey(key)) {
				values = ArrayUtils.add(map.get(key), value);
			} else {
				values = new String[] { value };
			}
			map.put(key, values);
		}
		return CatgraphConverter.convertParameters(map);
	}

	/**
	 * Test for the {@link CatgraphConverter#convertParameters(Map)} method.
	 */
	@Test
	public void testConvertParameters() {

		Map<String, String[]> out = convertMap();
		assertTrue(out.isEmpty());

		/* wiki */

		// Wiki db name
		out = convertMap("wiki", "dewiki");
		assertEquals(1, out.size());
		assertSingleValue("dewiki", out, AbstractAllParams.PARAM_WIKI);

		// Wiki plus language
		out = convertMap("wiki", "wikipedia", "lang", "en");
		assertEquals(1, out.size());
		assertSingleValue("enwiki", out, AbstractAllParams.PARAM_WIKI);
		for (String wiki : WIKI_LIST_LANGUAGE) {
			out = convertMap("wiki", wiki, "lang", "fr");
			assertEquals(1, out.size());
			assertSingleValue("fr" + wiki, out, AbstractAllParams.PARAM_WIKI);
		}

		// Unrecognized wiki loses language
		out = convertMap("wiki", "xyz", "lang", "en");
		assertEquals(1, out.size());
		assertSingleValue("xyz", out, AbstractAllParams.PARAM_WIKI);

		// Wikis with no language
		for (String wiki : WIKI_LIST_NOLANGUAGE) {
			out = convertMap("wiki", wiki);
			assertEquals(1, out.size());
			assertSingleValue(wiki + "wiki", out, AbstractAllParams.PARAM_WIKI);
		}

		/* cat and ns */

		// Only cat
		out = convertMap("cat", "test");
		assertEquals(1, out.size());
		assertSingleValue("test", out, AbstractAllParams.PARAM_CATEGORY);

		// Multiple
		out = convertMap("cat", "test1", "cat", "test2", "cat", "test3");
		assertEquals(1, out.size());
		final String[] values = out.get(AbstractAllParams.PARAM_CATEGORY);
		assertNotNull(values);
		assertEquals(3, values.length);
		assertTrue(ArrayUtils.contains(values, "test1"));
		assertTrue(ArrayUtils.contains(values, "test2"));
		assertTrue(ArrayUtils.contains(values, "test3"));

		// Cat with ns 14
		out = convertMap("cat", "test", "ns", "14");
		assertEquals(1, out.size());
		assertSingleValue("test", out, AbstractAllParams.PARAM_CATEGORY);

		// Cat with namespace 0
		out = convertMap("cat", "test", "ns", "0");
		assertEquals(2, out.size());
		assertSingleValue("test", out, AbstractAllParams.PARAM_TITLE);
		assertSingleValue("0", out, AbstractAllParams.PARAM_NAMESPACE);

		/* d */

		out = convertMap("d", "0");
		assertTrue(out.isEmpty());

		out = convertMap("d", "invalid");
		assertSingleValue("invalid", out, AbstractAllParams.PARAM_DEPTH);

		out = convertMap("d", "1");
		assertEquals(1, out.size());
		assertSingleValue("1", out, AbstractAllParams.PARAM_DEPTH);

		/* n */

		out = convertMap("n", "0");
		assertTrue(out.isEmpty());

		out = convertMap("n", "invalid");
		assertSingleValue("invalid", out, AbstractAllParams.PARAM_LIMIT);

		out = convertMap("n", "1");
		assertEquals(1, out.size());
		assertSingleValue("1", out, AbstractAllParams.PARAM_LIMIT);

		/* sub */

		// Ignore sub for false values
		for (String falseString : FALSE_VALUES) {
			out = convertMap("sub", falseString);
			assertTrue(out.isEmpty());
		}

		// Convertion to rel
		for (String trueString : TRUE_VALUES) {
			out = convertMap("sub", trueString);
			assertEquals(1, out.size());
			assertSingleValue("subcategory", out, AbstractAllParams.PARAM_RELATION);
		}

		// Automatically change ns for sub=article
		out = convertMap("sub", "article");
		assertEquals(1, out.size());
		assertSingleValue("0", out, AbstractAllParams.PARAM_NAMESPACE);

		/* fdp */

		// Ignore false values
		for (String falseString : FALSE_VALUES) {
			out = convertMap("fdp", falseString);
			assertTrue(out.isEmpty());
		}

		// Convertion to algorithm
		for (String trueString : TRUE_VALUES) {
			out = convertMap("fdp", trueString);
			assertEquals(1, out.size());
			assertSingleValue("fdp", out, AbstractAllParams.PARAM_ALGORITHM);
		}

		/* links */

		// Ignore false values
		for (String falseString : FALSE_VALUES) {
			out = convertMap("links", falseString);
			assertTrue(out.isEmpty());
		}

		// Conversion to graph
		for (String trueString : TRUE_VALUES) {
			out = convertMap("links", trueString);
			assertEquals(1, out.size());
			assertSingleValue("graph", out, AbstractAllParams.PARAM_LINKS);
		}

		// Conversion to wiki
		out = convertMap("links", "wiki");
		assertEquals(1, out.size());
		assertSingleValue("wiki", out, AbstractAllParams.PARAM_LINKS);

		/* format */

		for (OutputFormat format : OutputFormat.values()) {
			final String[] parameterNames = format.getParameterNames();
			for (String parameterName : parameterNames) {
				out = convertMap("format", parameterName);
				if ("png".equalsIgnoreCase(parameterName)) {
					// png is removed
					assertTrue(out.isEmpty());
				} else {
					// others are kept
					assertEquals(1, out.size());
					assertSingleValue(parameterName, out, AbstractAllParams.PARAM_FORMAT);
				}
			}
		}

		/* ignorehidden */

		// Set showhidden if false
		for (String falseString : FALSE_VALUES) {
			out = convertMap("ignorehidden", falseString);
			assertEquals(1, out.size());
			assertSingleValue("1", out, AbstractAllParams.PARAM_SHOWHIDDEN);

		}

		// Ignore true falues
		for (String trueString : TRUE_VALUES) {
			out = convertMap("ignorehidden", trueString);
			assertTrue(out.isEmpty());
		}

		/* small */

		// Always removed
		for (String falseString : FALSE_VALUES) {
			out = convertMap("small", falseString);
			assertTrue(out.isEmpty());
		}
		for (String falseString : TRUE_VALUES) {
			out = convertMap("small", falseString);
			assertTrue(out.isEmpty());
		}

		/* A few complex tests */

		out = convertMap("wiki", "dewiki", "cat", "Köln", "sub", "article", "format", "gif", "ignorehidden", "1",
				"links", "0");
		assertEquals(4, out.size());
		assertSingleValue("dewiki", out, AbstractAllParams.PARAM_WIKI);
		assertSingleValue("Köln", out, AbstractAllParams.PARAM_TITLE);
		assertSingleValue("0", out, AbstractAllParams.PARAM_NAMESPACE);
		assertSingleValue("gif", out, AbstractAllParams.PARAM_FORMAT);

		out = convertMap("wiki", "wikipedia", "lang", "de", "cat", "Düsseldorf", "ns", "0", "format", "svg",
				"ignorehidden", "0", "links", "yesplease");
		assertEquals(6, out.size());
		assertSingleValue("dewiki", out, AbstractAllParams.PARAM_WIKI);
		assertSingleValue("Düsseldorf", out, AbstractAllParams.PARAM_TITLE);
		assertSingleValue("0", out, AbstractAllParams.PARAM_NAMESPACE);
		assertSingleValue("svg", out, AbstractAllParams.PARAM_FORMAT);
		assertSingleValue("1", out, AbstractAllParams.PARAM_SHOWHIDDEN);
		assertSingleValue("graph", out, AbstractAllParams.PARAM_LINKS);

	}
}
