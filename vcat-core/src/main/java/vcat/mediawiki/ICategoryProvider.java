package vcat.mediawiki;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ICategoryProvider<W extends IWiki> {

	/**
	 * Get categories for the supplied collection of pages.
	 * 
	 * @param wiki
	 *            Wiki to get categories from.
	 * @param fullTitles
	 *            Full titles (including namespace) of pages.
	 * @param showhidden
	 *            Whether categories marked as hidden in the wiki (using <code>__HIDDENCAT__</code> should be included.
	 * @return Map of strings with the full titles (with namespace) of categories the page with the supplied full title
	 *         is in.
	 * @throws ApiException
	 *             If there are any errors.
	 */
	public abstract Map<String, Collection<String>> requestCategories(W wiki, Collection<String> fullTitles,
			boolean showhidden) throws ApiException;

	/**
	 * Get category members for the supplied category.
	 * 
	 * @param wiki
	 *            Wiki to get category members from.
	 * @param fullTitle
	 *            Full page title of the category.
	 * @return List of titles of the pages belonging to the category.
	 * @throws ApiException
	 *             If there are any errors.
	 */
	public abstract List<String> requestCategorymembers(W wiki, String fullTitle) throws ApiException;

}