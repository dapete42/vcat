package vcat.mediawiki.interfaces;

import vcat.mediawiki.ApiException;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CategoryProvider extends Serializable {

    /**
     * Get categories for the supplied collection of pages.
     *
     * @param wiki       Wiki to get categories from.
     * @param fullTitles Full titles (including namespace) of pages.
     * @param showhidden Whether categories marked as hidden in the wiki (using <code>__HIDDENCAT__</code>) should be included.
     * @return Map of strings with the full titles (with namespace) of categories the page with the supplied full title
     * is in.
     * @throws ApiException If there are any errors.
     */
    Map<String, Collection<String>> requestCategories(Wiki wiki, List<String> fullTitles,
                                                      boolean showhidden) throws ApiException;

    /**
     * Get category members for the supplied category.
     *
     * @param wiki      Wiki to get category members from.
     * @param fullTitle Full page title of the category.
     * @return List of titles of the pages belonging to the category.
     * @throws ApiException If there are any errors.
     */
    List<String> requestCategorymembers(Wiki wiki, String fullTitle) throws ApiException;

}