package vcat.mediawiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.collect.Lists;

import vcat.Messages;

public class ApiClient<W extends IWiki> implements ICategoryProvider<W>, IMetadataProvider {

	private static final long serialVersionUID = -3737560080769969852L;

	/** Maximum number of titles parameters to use in one request. */
	private static final int TITLES_MAX = 50;

	private final HttpClientBuilder clientBuilder;

	public ApiClient() {
		final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(5);
		/*
		 * API Etiquette: There should always be only one concurrent HTTP access; at least for Wikimedia-run wikis,
		 * doing more at the same time may lead to being blocked. It is probably nice to do this for any wiki, anyway.
		 * See https://www.mediawiki.org/wiki/API:Etiquette for more information on Wikimedia API etiquette.
		 */
		connectionManager.setDefaultMaxPerRoute(1);

		this.clientBuilder = HttpClientBuilder.create().setConnectionManager(connectionManager)
				.setConnectionManagerShared(true).setUserAgent(Messages.getString("ApiClient.UserAgent"))
				.disableCookieManagement();
	}

	private static Map<String, String> buildContinueMap(JsonObject result, final String oldQueryContinueChildName) {
		Map<String, String> continueMap = null;
		JsonObject jsonContinue = null;
		if (result.containsKey("continue")) {
			// new continue (default starting with MW 1.26)
			jsonContinue = result.getJsonObject("continue");
		} else if (result.containsKey("query-continue")) {
			// Old "raw" continue (default until MW 1.25)
			jsonContinue = result.getJsonObject("query-continue").getJsonObject(oldQueryContinueChildName);
		}
		if (jsonContinue != null) {
			continueMap = new HashMap<>();
			for (String key : jsonContinue.keySet()) {
				continueMap.put(key, jsonContinue.getString(key));
			}
		}
		return continueMap;
	}

	protected JsonObject request(String apiUrl, Map<String, String> params) throws ApiException {
		// API Etiquette: use "get" method to enable server-side caching
		final RequestBuilder requestBuilder = RequestBuilder.get(apiUrl);
		requestBuilder.setCharset(StandardCharsets.UTF_8);
		requestBuilder.addParameter("format", "json");
		requestBuilder.addParameter("action", "query");

		for (Entry<String, String> param : params.entrySet()) {
			requestBuilder.addParameter(param.getKey(), param.getValue());
		}

		final HttpUriRequest request = requestBuilder.build();

		try (CloseableHttpClient client = this.clientBuilder.build();
				CloseableHttpResponse response = client.execute(request);
				InputStream inputStream = response.getEntity().getContent();
				JsonReader jsonReader = Json.createReader(inputStream)) {
			return jsonReader.readObject();
		} catch (IllegalStateException | IOException e) {
			// IOException will be thrown for all HTTP problems
			throw new ApiException(MessageFormatter
					.format(Messages.getString("ApiClient.Exception.HTTP"), request.getURI().toString()).getMessage(),
					e);
		} catch (JsonException e) {
			throw new ApiException(MessageFormatter
					.format(Messages.getString("ApiClient.Exception.ParsingJSON"), request.getURI().toString())
					.getMessage(), e);
		}
	}

	@Override
	public Map<String, Collection<String>> requestCategories(W wiki, List<String> fullTitles, boolean showhidden)
			throws ApiException {
		Map<String, Collection<String>> categoryMap = new HashMap<>();
		String clshow = showhidden ? null : "!hidden";
		this.requestCategoriesRecursive(wiki.getApiUrl(), fullTitles, categoryMap, null, clshow);
		return categoryMap;
	}

	private void requestCategoriesRecursive(String apiUrl, List<String> fullTitles,
			final Map<String, Collection<String>> categoryMap, Map<String, String> continueMap, String clshow)
			throws ApiException {

		if (fullTitles.size() > TITLES_MAX) {

			for (List<String> fullTitlesPart : Lists.partition(fullTitles, TITLES_MAX)) {
				requestCategoriesRecursive(apiUrl, fullTitlesPart, categoryMap, continueMap, clshow);
			}

		} else {

			// Set query properties
			Map<String, String> params = new HashMap<>();
			params.put("prop", "categories");
			params.put("cllimit", "max");
			params.put("titles", StringUtils.join(fullTitles, '|'));
			if (clshow != null) {
				params.put("clshow", clshow);
			}
			if (continueMap != null) {
				params.putAll(continueMap);
			}
			JsonObject result = this.request(apiUrl, params);

			try {
				JsonObject query = result.getJsonObject("query");
				if (query != null) {
					JsonObject pages = query.getJsonObject("pages");
					if (pages != null) {
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
					}
				}

				// Try to get continue map, if there is one, make another request
				final Map<String, String> newContinueMap = buildContinueMap(result, "categories");
				if (newContinueMap != null) {
					this.requestCategoriesRecursive(apiUrl, fullTitles, categoryMap, newContinueMap, clshow);
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
			final List<String> categories, final Map<String, String> continueMap) throws ApiException {

		// Set query properties
		Map<String, String> params = new HashMap<>();
		params.put("list", "categorymembers");
		params.put("cmlimit", "max");
		params.put("cmtitle", fullTitle);
		params.put("cmtype", "subcat");
		if (continueMap != null) {
			params.putAll(continueMap);
		}
		JsonObject result = this.request(apiUrl, params);

		try {
			JsonObject query = result.getJsonObject("query");
			if (query != null) {
				JsonArray jsonCategories = query.getJsonArray("categorymembers");
				if (jsonCategories != null) {
					for (int i = 0; i < jsonCategories.size(); i++) {
						JsonObject category = jsonCategories.getJsonObject(i);
						categories.add(category.getString("title"));
					}
				}
			}

			// Try to get continue map, if there is one, make another request
			final Map<String, String> newContinueMap = buildContinueMap(result, "categorymembers");
			if (newContinueMap != null) {
				this.requestCategorymembersRecursive(apiUrl, fullTitle, categories, newContinueMap);
			}
		} catch (JsonException e) {
			throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
		}

	}

	public Collection<Pair<String, String>> requestLinksBetween(W wiki, List<String> fullTitles) throws ApiException {
		ArrayList<Pair<String, String>> links = new ArrayList<>();
		this.requestLinksBetweenRecursive(wiki.getApiUrl(), fullTitles, links, null);
		return links;
	}

	private void requestLinksBetweenRecursive(final String apiUrl, final List<String> fullTitles,
			final Collection<Pair<String, String>> links, Map<String, String> continueMap) throws ApiException {

		if (fullTitles.size() > TITLES_MAX) {

			for (List<String> fullTitlesPart : Lists.partition(fullTitles, TITLES_MAX)) {
				requestLinksBetweenRecursive(apiUrl, fullTitlesPart, links, continueMap);
			}

		} else {

			// Set query properties
			Map<String, String> params = new HashMap<>();
			params.put("prop", "links");
			params.put("pllimit", "max");
			final String titlesParam = StringUtils.join(fullTitles, '|');
			params.put("titles", titlesParam);
			params.put("pltitles", titlesParam);
			if (continueMap != null) {
				params.putAll(continueMap);
			}
			JsonObject result = this.request(apiUrl, params);

			try {
				JsonObject query = result.getJsonObject("query");
				if (query != null) {
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
				}

				// Try to get continue map, if there is one, make another request
				final Map<String, String> newContinueMap = buildContinueMap(result, "categories");
				if (newContinueMap != null) {
					this.requestLinksBetweenRecursive(apiUrl, fullTitles, links, newContinueMap);
				}
			} catch (JsonException e) {
				throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
			}

		}
	}

	@Override
	public Metadata requestMetadata(final IWiki wiki) throws ApiException {
		// Set query properties
		Map<String, String> params = new HashMap<>();
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
