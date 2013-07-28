package vcat.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import vcat.util.CollectionHelper;

/**
 * JUnit tests for the {@link CollectionHelper} class.
 * 
 * @author Peter Schlömer
 */
public class CollectionHelperTest {

	/**
	 * Test for the {@link CollectionHelper#splitCollectionInParts(java.util.Collection, int)} method.
	 */
	@Test
	public void testSplitCollectionInParts() {

		// Null and empty list become empty lists
		assertEquals(0, CollectionHelper.splitCollectionInParts(null, 100).size());
		assertEquals(0, CollectionHelper.splitCollectionInParts(new ArrayList<String>(), 100).size());

		// Some test data
		ArrayList<String> testData = new ArrayList<String>(10);
		testData.add("test0");
		testData.add("test1");
		testData.add("test2");
		testData.add("test3");
		testData.add("test4");
		testData.add("test5");
		testData.add("test6");
		testData.add("test7");
		testData.add("test8");
		testData.add("test9");
		assertEquals(10, testData.size());

		// Split into collections with 1 item each and check all values
		{
			Collection<Collection<String>> testResult = CollectionHelper.splitCollectionInParts(testData, 1);
			assertEquals(10, testResult.size());
			int i = 0;
			for (Collection<String> strings : testResult) {
				assertEquals(1, strings.size());
				assertTrue(strings.contains(testData.get(i)));
				i++;
			}
		}

		// Split into collections with 2 items each and check all values
		{
			Collection<Collection<String>> testResult = CollectionHelper.splitCollectionInParts(testData, 2);
			assertEquals(5, testResult.size());
			int i = 0;
			for (Collection<String> strings : testResult) {
				assertEquals(2, strings.size());
				assertTrue(strings.contains(testData.get(i)));
				assertTrue(strings.contains(testData.get(i + 1)));
				i += 2;
			}
		}

		// Split into collections with 3 items each and check all values
		{
			Collection<Collection<String>> testResult = CollectionHelper.splitCollectionInParts(testData, 3);
			assertEquals(4, testResult.size());
			int i = 0;
			for (Collection<String> strings : testResult) {
				if (i < 9) {
					assertEquals(3, strings.size());
				} else {
					assertEquals(1, strings.size());
				}
				assertTrue(strings.contains(testData.get(i)));
				if (i < 9) {
					assertTrue(strings.contains(testData.get(i + 1)));
					assertTrue(strings.contains(testData.get(i + 2)));
				}
				i += 3;
			}
		}

		// Split into collections up until 9 and check only the number of parts
		{
			Collection<Collection<String>> testResult = CollectionHelper.splitCollectionInParts(testData, 4);
			assertEquals(3, testResult.size());
			for (int i = 5; i < 10; i++) {
				testResult = CollectionHelper.splitCollectionInParts(testData, i);
				assertEquals(2, testResult.size());
			}
		}

		// "Split" into one collection; happens for corner cases of 10/11 but also for zero and negative values
		for (int i : new int[] { -1, 0, 10, 11 }) {
			Collection<Collection<String>> testResult = CollectionHelper.splitCollectionInParts(testData, i);
			assertEquals(1, testResult.size());
			for (Collection<String> strings : testResult) {
				assertEquals(10, strings.size());
				for (int j = 0; j < 10; j++) {
					assertTrue(strings.contains(testData.get(j)));
				}
			}
		}

	}
}
