package vcat.mediawiki;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApiClientTest {

	/* Use German Wikipedia for testing. */

	private final static String PAGE_1 = "Deutschland";

	private final static String PAGE_2 = "Frankreich";

	private final static String CATEGORY_1 = "Kategorie:Deutschland";

	private final static String WIKI = "de.wikipedia.org";

	private IWiki wiki;

	private ApiClient<IWiki> client;

	@BeforeEach
	public void before() {
		this.wiki = new SimpleWikimediaWiki(WIKI);
		this.client = new ApiClient<>();
	}

	@Test
	public void testRequestCategories() throws ApiException {
		final List<String> fullTitles = new ArrayList<>();
		fullTitles.add(PAGE_1);
		fullTitles.add(PAGE_2);
		final Map<String, Collection<String>> result = this.client.requestCategories(wiki, fullTitles, true);
		assertNotNull(result);
		assertNotEquals(0, result.size());
	}

	@Test
	public void testRequestCategorymembers() throws ApiException {
		final List<String> result = this.client.requestCategorymembers(wiki, CATEGORY_1);
		assertNotNull(result);
		assertNotEquals(0, result.size());
	}

	@Test
	public void testRequestLinksBetween() throws ApiException {
		final List<String> fullTitles = new ArrayList<>();
		fullTitles.add(PAGE_1);
		fullTitles.add(PAGE_2);
		final Collection<Pair<String, String>> result = client.requestLinksBetween(wiki, fullTitles);
		assertNotNull(result);
		assertNotEquals(0, result.size());
	}

	@Test
	public void testRequestMetadata() throws ApiException {
		Metadata result = client.requestMetadata(wiki);
		assertNotNull(result);
	}

}
