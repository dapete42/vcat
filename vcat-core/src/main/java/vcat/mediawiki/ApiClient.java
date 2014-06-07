package vcat.mediawiki;

import in.yuvi.http.fluent.Http;
import in.yuvi.http.fluent.Http.HttpRequestBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import vcat.Messages;
import vcat.util.CollectionHelper;

public class ApiClient<W extends IWiki> implements ICategoryProvider<W>, IMetadataProvider {

	/** Maximum number of titles parameters to use in one request. */
	private final static int TITLES_MAX = 50;

	private DefaultHttpClient client;

	public ApiClient() {
		this.client = new DefaultHttpClient();
		this.client.getParams().setParameter("User-Agent", Messages.getString("ApiClient.UserAgent"));
	}

	protected JSONObject request(String apiUrl, Map<String, String> params) throws ApiException {
		// API Etiquette: use get to enable server-side caching
		HttpRequestBuilder builder = Http.get(apiUrl);
		builder.use(this.client).charset("utf-8");
		builder.data("format", "json");
		builder.data("action", "query");
		builder.data(params);

		/*
		 * API Etiquette: There should always be only one concurrent HTTP access; at least for Wikimedia-run wikis,
		 * doing more at the same time may lead to being blocked. It is probably nice to do this for any wiki, anyway.
		 */
		synchronized (ApiClient.class) {
			InputStream content;
			try {
				HttpResponse response = builder.asResponse();
				HttpEntity entity = response.getEntity();
				content = entity.getContent();
			} catch (IOException e) {
				throw new ApiException(Messages.getString("ApiClient.Exception.HTTP"), e);
			}
			InputStreamReader reader = new InputStreamReader(content);
			try {
				JSONObject result = new JSONObject(new JSONTokener(reader));
				reader.close();
				return result;
			} catch (Exception e) {
				throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
			}
		}
	}

	@Override
	public Map<String, Collection<String>> requestCategories(W wiki, Collection<String> fullTitles, boolean showhidden)
			throws ApiException {
		HashMap<String, Collection<String>> categorySet = new HashMap<String, Collection<String>>();
		String clshow = showhidden ? null : "!hidden";
		this.requestCategoriesRecursive(wiki.getApiUrl(), fullTitles, categorySet, null, clshow);
		return categorySet;
	}

	private void requestCategoriesRecursive(String apiUrl, Collection<String> fullTitles,
			final Map<String, Collection<String>> categoryMap, String clcontinue, String clshow) throws ApiException {

		if (fullTitles.size() > TITLES_MAX) {

			for (Collection<String> fullTitlesPart : CollectionHelper.splitCollectionInParts(fullTitles, TITLES_MAX)) {
				requestCategoriesRecursive(apiUrl, fullTitlesPart, categoryMap, clcontinue, clshow);
			}

		} else {

			// Set query properties
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("prop", "categories");
			params.put("cllimit", "max");
			params.put("titles", StringUtils.join(fullTitles, '|'));
			if (clshow != null) {
				params.put("clshow", clshow);
			}
			if (clcontinue != null) {
				params.put("clcontinue", clcontinue);
			}
			JSONObject result = this.request(apiUrl, params);

			try {
				JSONObject query = result.getJSONObject("query");
				JSONObject pages = query.getJSONObject("pages");
				for (String pagesKey : JSONObject.getNames(pages)) {
					JSONObject pagesData = pages.getJSONObject(pagesKey);
					if (pagesData.has("categories")) {
						JSONArray jsonCategories = pagesData.getJSONArray("categories");
						String pagesDataTitle = pagesData.getString("title");
						Collection<String> categories = categoryMap.get(pagesDataTitle);
						if (categories == null) {
							categories = new ArrayList<String>(jsonCategories.length());
							categoryMap.put(pagesDataTitle, categories);
						}
						for (int i = 0; i < jsonCategories.length(); i++) {
							JSONObject category = jsonCategories.getJSONObject(i);
							categories.add(category.getString("title"));
						}
					}
				}

				// If query-continue was returned, make another request
				if (result.has("query-continue")) {
					this.requestCategoriesRecursive(apiUrl, fullTitles, categoryMap,
							result.getJSONObject("query-continue").getJSONObject("categories").getString("clcontinue"),
							clshow);
				}
			} catch (JSONException e) {
				throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
			}

		}
	}

	@Override
	public List<String> requestCategorymembers(final IWiki wiki, final String fullTitle) throws ApiException {
		List<String> categories = new ArrayList<String>();
		this.requestCategorymembersRecursive(wiki.getApiUrl(), fullTitle, categories, null);
		return categories;
	}

	private void requestCategorymembersRecursive(final String apiUrl, final String fullTitle,
			final List<String> categories, final String cmcontinue) throws ApiException {

		// Set query properties
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("list", "categorymembers");
		params.put("cmlimit", "max");
		params.put("cmtitle", fullTitle);
		params.put("cmtype", "subcat");
		if (cmcontinue != null) {
			params.put("cmcontinue", cmcontinue);
		}
		JSONObject result = this.request(apiUrl, params);

		try {
			JSONObject query = result.getJSONObject("query");
			if (query.has("categorymembers")) {
				JSONArray jsonCategories = query.getJSONArray("categorymembers");
				for (int i = 0; i < jsonCategories.length(); i++) {
					JSONObject category = jsonCategories.getJSONObject(i);
					categories.add(category.getString("title"));
				}
			}

			// If query-continue was returned, make another request
			if (result.has("query-continue")) {
				this.requestCategorymembersRecursive(apiUrl, fullTitle, categories,
						result.getJSONObject("query-continue").getJSONObject("categorymembers").getString("cmcontinue"));
			}
		} catch (JSONException e) {
			throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
		}

	}

	@Override
	public Metadata requestMetadata(final IWiki wiki) throws ApiException {
		// Set query properties
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("meta", "siteinfo");
		params.put("siprop", "general|namespaces|namespacealiases");

		JSONObject json = this.request(wiki.getApiUrl(), params);

		String articlepath;
		String server;
		final Map<Integer, String> authoritativeNamespaces = new HashMap<Integer, String>();
		final Map<String, Integer> allNamespacesInverse = new HashMap<String, Integer>();

		try {
			JSONObject query = json.getJSONObject("query");

			articlepath = query.getJSONObject("general").getString("articlepath");

			server = query.getJSONObject("general").getString("server");

			JSONObject namespaces = query.getJSONObject("namespaces");
			for (String nsIdString : JSONObject.getNames(namespaces)) {
				JSONObject namespaceData = namespaces.getJSONObject(nsIdString);
				int nsId = Integer.parseInt(nsIdString);
				String nsName = namespaceData.getString("*");
				authoritativeNamespaces.put(nsId, nsName);
				allNamespacesInverse.put(nsName, nsId);
				if (namespaceData.has("canonical")) {
					String nsCanonical = namespaceData.getString("canonical");
					if (nsCanonical != null && !nsCanonical.isEmpty()) {
						allNamespacesInverse.put(nsCanonical, nsId);
					}
				}
			}

			JSONArray namespacealiases = query.getJSONArray("namespacealiases");
			for (int i = 0; i < namespacealiases.length(); i++) {
				JSONObject namespacealiasData = namespacealiases.getJSONObject(i);
				int nsId = namespacealiasData.getInt("id");
				String nsName = namespacealiasData.getString("*");
				if (nsName != null && !nsName.isEmpty()) {
					allNamespacesInverse.put(nsName, nsId);
				}
			}

		} catch (JSONException e) {
			throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
		}

		return new Metadata(wiki, articlepath, server, authoritativeNamespaces, allNamespacesInverse);

	}
}
