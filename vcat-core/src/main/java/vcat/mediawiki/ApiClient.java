package vcat.mediawiki;

import in.yuvi.http.fluent.Http;
import in.yuvi.http.fluent.Http.HttpRequestBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import vcat.Messages;
import vcat.util.CollectionHelper;

public class ApiClient<W extends IWiki> implements ICategoryProvider<W>, IMetadataProvider {

	/** Maximum number of titles parameters to use in one request. */
	private final static int TITLES_MAX = 50;

	private final HttpClient client;

	public ApiClient() {
		this.client = new DefaultHttpClient();
		this.client.getParams().setParameter("User-Agent", Messages.getString("ApiClient.UserAgent"));
	}

	protected JsonObject request(String apiUrl, Map<String, String> params) throws ApiException {
		// API Etiquette: use get to enable server-side caching
		HttpRequestBuilder builder = Http.get(apiUrl);
		builder.use(this.client).charset("utf-8");
		builder.data("format", "json");
		builder.data("action", "query");
		builder.data(params);

		/*
		 * API Etiquette: There should always be only one concurrent HTTP access; at least for Wikimedia-run wikis,
		 * doing more at the same time may lead to being blocked. It is probably nice to do this for any wiki, anyway.
		 * See https://www.mediawiki.org/wiki/API:Etiquette for more information on Wikimedia API etiquette.
		 */
		synchronized (ApiClient.class) {

			try (final InputStreamReader reader = new InputStreamReader(builder.asResponse().getEntity().getContent())) {
				return Json.createReader(reader).readObject();
			} catch (IOException e) {
				// IOException will be thrown for all HTTP problems
				throw new ApiException(Messages.getString("ApiClient.Exception.HTTP"), e);
			} catch (JsonException e) {
				throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
			}

		}
	}

	@Override
	public Map<String, Collection<String>> requestCategories(W wiki, Collection<String> fullTitles, boolean showhidden)
			throws ApiException {
		HashMap<String, Collection<String>> categoryMap = new HashMap<>();
		String clshow = showhidden ? null : "!hidden";
		this.requestCategoriesRecursive(wiki.getApiUrl(), fullTitles, categoryMap, null, clshow);
		return categoryMap;
	}

	private void requestCategoriesRecursive(String apiUrl, Collection<String> fullTitles,
			final Map<String, Collection<String>> categoryMap, String clcontinue, String clshow) throws ApiException {

		if (fullTitles.size() > TITLES_MAX) {

			for (Collection<String> fullTitlesPart : CollectionHelper.splitCollectionInParts(fullTitles, TITLES_MAX)) {
				requestCategoriesRecursive(apiUrl, fullTitlesPart, categoryMap, clcontinue, clshow);
			}

		} else {

			// Set query properties
			HashMap<String, String> params = new HashMap<>();
			params.put("prop", "categories");
			params.put("cllimit", "max");
			params.put("titles", StringUtils.join(fullTitles, '|'));
			if (clshow != null) {
				params.put("clshow", clshow);
			}
			if (clcontinue != null) {
				params.put("clcontinue", clcontinue);
			}
			JsonObject result = this.request(apiUrl, params);

			try {
				JsonObject query = result.getJsonObject("query");
				JsonObject pages = query.getJsonObject("pages");
				for (String pagesKey : pages.keySet()) {
					JsonObject pagesData = pages.getJsonObject(pagesKey);
					if (pagesData.containsKey("categories")) {
						JsonArray jsonCategories = pagesData.getJsonArray("categories");
						String pagesDataTitle = pagesData.getString("title");
						Collection<String> categories = categoryMap.get(pagesDataTitle);
						if (categories == null) {
							categories = new ArrayList<>(jsonCategories.size());
							categoryMap.put(pagesDataTitle, categories);
						}
						for (int i = 0; i < jsonCategories.size(); i++) {
							JsonObject category = jsonCategories.getJsonObject(i);
							categories.add(category.getString("title"));
						}
					}
				}

				// If query-continue was returned, make another request
				if (result.containsKey("query-continue")) {
					this.requestCategoriesRecursive(apiUrl, fullTitles, categoryMap,
							result.getJsonObject("query-continue").getJsonObject("categories").getString("clcontinue"),
							clshow);
				}
			} catch (JsonException e) {
				throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
			}

		}
	}

	@Override
	public List<String> requestCategorymembers(final W wiki, final String fullTitle) throws ApiException {
		List<String> categories = new ArrayList<>();
		this.requestCategorymembersRecursive(wiki.getApiUrl(), fullTitle, categories, null);
		return categories;
	}

	private void requestCategorymembersRecursive(final String apiUrl, final String fullTitle,
			final List<String> categories, final String cmcontinue) throws ApiException {

		// Set query properties
		HashMap<String, String> params = new HashMap<>();
		params.put("list", "categorymembers");
		params.put("cmlimit", "max");
		params.put("cmtitle", fullTitle);
		params.put("cmtype", "subcat");
		if (cmcontinue != null) {
			params.put("cmcontinue", cmcontinue);
		}
		JsonObject result = this.request(apiUrl, params);

		try {
			JsonObject query = result.getJsonObject("query");
			if (query.containsKey("categorymembers")) {
				JsonArray jsonCategories = query.getJsonArray("categorymembers");
				for (int i = 0; i < jsonCategories.size(); i++) {
					JsonObject category = jsonCategories.getJsonObject(i);
					categories.add(category.getString("title"));
				}
			}

			// If query-continue was returned, make another request
			if (result.containsKey("query-continue")) {
				this.requestCategorymembersRecursive(apiUrl, fullTitle, categories,
						result.getJsonObject("query-continue").getJsonObject("categorymembers").getString("cmcontinue"));
			}
		} catch (JsonException e) {
			throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
		}

	}

	public Collection<Pair<String, String>> requestLinksBetween(W wiki, Collection<String> fullTitles)
			throws ApiException {
		ArrayList<Pair<String, String>> links = new ArrayList<>();
		this.requestLinksBetweenRecursive(wiki.getApiUrl(), fullTitles, links, null);
		return links;
	}

	private void requestLinksBetweenRecursive(String apiUrl, Collection<String> fullTitles,
			final Collection<Pair<String, String>> links, String plcontinue) throws ApiException {

		if (fullTitles.size() > TITLES_MAX) {

			for (Collection<String> fullTitlesPart : CollectionHelper.splitCollectionInParts(fullTitles, TITLES_MAX)) {
				requestLinksBetweenRecursive(apiUrl, fullTitlesPart, links, plcontinue);
			}

		} else {

			// Set query properties
			HashMap<String, String> params = new HashMap<>();
			params.put("prop", "links");
			params.put("pllimit", "max");
			final String titlesParam = StringUtils.join(fullTitles, '|');
			params.put("titles", titlesParam);
			params.put("pltitles", titlesParam);
			if (plcontinue != null) {
				params.put("plcontinue", plcontinue);
			}
			JsonObject result = this.request(apiUrl, params);

			try {
				JsonObject query = result.getJsonObject("query");
				JsonObject pages = query.getJsonObject("pages");
				for (String pagesKey : pages.keySet()) {
					JsonObject pagesData = pages.getJsonObject(pagesKey);
					if (pagesData.containsKey("links")) {
						JsonArray jsonLinks = pagesData.getJsonArray("links");
						String pagesDataTitle = pagesData.getString("title");
						for (int i = 0; i < jsonLinks.size(); i++) {
							JsonObject category = jsonLinks.getJsonObject(i);
							links.add(new MutablePair<>(pagesDataTitle, category.getString("title")));
						}
					}
				}

				// If query-continue was returned, make another request
				if (result.containsKey("query-continue")) {
					this.requestLinksBetweenRecursive(apiUrl, fullTitles, links, result.getJsonObject("query-continue")
							.getJsonObject("categories").getString("plcontinue"));
				}
			} catch (JsonException e) {
				throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
			}

		}
	}

	@Override
	public Metadata requestMetadata(final IWiki wiki) throws ApiException {
		// Set query properties
		HashMap<String, String> params = new HashMap<>();
		params.put("meta", "siteinfo");
		params.put("siprop", "general|namespaces|namespacealiases");

		JsonObject json = this.request(wiki.getApiUrl(), params);

		String articlepath;
		String server;
		final Map<Integer, String> authoritativeNamespaces = new HashMap<>();
		final Map<String, Integer> allNamespacesInverse = new HashMap<>();

		try {
			JsonObject query = json.getJsonObject("query");

			articlepath = query.getJsonObject("general").getString("articlepath");

			server = query.getJsonObject("general").getString("server");

			JsonObject namespaces = query.getJsonObject("namespaces");
			for (String nsIdString : namespaces.keySet()) {
				JsonObject namespaceData = namespaces.getJsonObject(nsIdString);
				int nsId = Integer.parseInt(nsIdString);
				String nsName = namespaceData.getString("*");
				authoritativeNamespaces.put(nsId, nsName);
				allNamespacesInverse.put(nsName, nsId);
				if (namespaceData.containsKey("canonical")) {
					String nsCanonical = namespaceData.getString("canonical");
					if (nsCanonical != null && !nsCanonical.isEmpty()) {
						allNamespacesInverse.put(nsCanonical, nsId);
					}
				}
			}

			JsonArray namespacealiases = query.getJsonArray("namespacealiases");
			for (int i = 0; i < namespacealiases.size(); i++) {
				JsonObject namespacealiasData = namespacealiases.getJsonObject(i);
				int nsId = namespacealiasData.getInt("id");
				String nsName = namespacealiasData.getString("*");
				if (nsName != null && !nsName.isEmpty()) {
					allNamespacesInverse.put(nsName, nsId);
				}
			}

		} catch (JsonException e) {
			throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
		}

		return new Metadata(articlepath, server, authoritativeNamespaces, allNamespacesInverse);

	}
}
