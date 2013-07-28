package vcat.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionHelper {

	/**
	 * Splits a Collection in several parts with a maximum number of items.
	 * 
	 * @param collection
	 *            Collection to split
	 * @param numberOfItems
	 *            Maximum number of items in resulting List of Collections.
	 * @return List of Collections
	 */
	public static <T> List<Collection<T>> splitCollectionInParts(Collection<T> collection, int numberOfItems) {

		ArrayList<Collection<T>> collections;

		if (numberOfItems < 1) {
			// Number of items < 1 does not split
			collections = new ArrayList<Collection<T>>(1);
			collections.add(collection);
			return collections;
		} else if (collection == null) {
			// Null becomes an empty list
			return new ArrayList<Collection<T>>();
		}

		int collectionSize = collection.size();

		collections = new ArrayList<Collection<T>>(collectionSize / numberOfItems + 1);
		ArrayList<T> currentCollection = null;
		long i = 0;
		for (T item : collection) {
			if (i % numberOfItems == 0) {
				if (currentCollection != null) {
					collections.add(currentCollection);
				}
				currentCollection = new ArrayList<T>(numberOfItems);
			}
			currentCollection.add(item);
			i++;
		}
		if (currentCollection != null && !currentCollection.isEmpty()) {
			collections.add(currentCollection);
		}
		return collections;
	}

}
