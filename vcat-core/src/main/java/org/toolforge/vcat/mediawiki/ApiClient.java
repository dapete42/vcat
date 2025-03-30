package org.toolforge.vcat.mediawiki;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.UriBuilder;
import net.dapete.locks.Locks;
import net.dapete.locks.ReentrantLocks;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.slf4j.helpers.MessageFormatter;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.mediawiki.interfaces.CategoryProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;

public class ApiClient implements CategoryProvider, MetadataProvider {

    @Serial
    private static final long serialVersionUID = -5476476560276033334L;

    /**
     * Maximum number of titles parameters to use in one request.
     */
    private static final int TITLES_MAX = 50;

    /**
     * Locks for API Etiquette (see below).
     */
    private final ReentrantLocks<String> locks = Locks.reentrant(true);

    private final HttpClient httpClient;

    public ApiClient() {
        httpClient = HttpClient.newBuilder()
                .build();
    }

    @Nullable
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


    private static URI buildRequestUri(String apiUrl, Map<String, String> params) {
        final var uriBuilder = UriBuilder.fromUri(apiUrl)
                .queryParam("format", "json")
                .queryParam("action", "query");
        params.forEach(uriBuilder::queryParam);
        return uriBuilder.build();
    }

    private static HttpRequest buildHttpRequest(URI uri) {
        // API Etiquette: use default "get" method to enable server-side caching
        return HttpRequest.newBuilder(uri)
                .setHeader("User-Agent", Messages.getString("ApiClient.UserAgent"))
                .build();
    }

    @Nullable
    protected JsonObject request(String apiUrl, Map<String, String> params) throws ApiException {
        final var requestUri = buildRequestUri(apiUrl, params);
        final var httpRequest = buildHttpRequest(requestUri);

        /*
         * API Etiquette: There should always be only one concurrent HTTP access; at least for Wikimedia-run wikis, doing more at the same time may lead
         * to being blocked. It is probably nice to do this for any wiki, anyway. See <a href="https://www.mediawiki.org/wiki/API:Etiquette">API:Etiquette</a> on
         * the MediaWiki wiki for more information on Wikimedia API etiquette.
         */
        final var lock = locks.lock(apiUrl);
        try {
            final var httpResponse = httpClient.send(httpRequest, BodyHandlers.ofInputStream());
            try (var bodyStream = httpResponse.body();
                 var jsonReader = Json.createReader(bodyStream)) {
                return jsonReader.readObject();
            }
        } catch (InterruptedException | IOException e) {
            // IOException will be thrown for all HTTP problems
            throw new ApiException(MessageFormatter
                    .format(Messages.getString("ApiClient.Exception.HTTP"), requestUri.toString()).getMessage(),
                    e);
        } catch (JsonException e) {
            throw new ApiException(MessageFormatter
                    .format(Messages.getString("ApiClient.Exception.ParsingJSON"), requestUri.toString())
                    .getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<String, Collection<String>> requestCategories(Wiki wiki, List<String> fullTitles, boolean showHidden)
            throws ApiException {
        final Map<String, Collection<String>> categoryMap = new HashMap<>();
        final String clshow = showHidden ? null : "!hidden";
        requestCategoriesRecursive(wiki.getApiUrl(), fullTitles, categoryMap, null, clshow);
        return categoryMap;
    }

    private void requestCategoriesRecursive(String apiUrl, List<String> fullTitles, final Map<String, Collection<String>> categoryMap,
                                            @Nullable Map<String, String> continueMap, @Nullable String clshow) throws ApiException {
        if (fullTitles.size() > TITLES_MAX) {

            for (List<String> fullTitlesPart : ListUtils.partition(fullTitles, TITLES_MAX)) {
                requestCategoriesRecursive(apiUrl, fullTitlesPart, categoryMap, continueMap, clshow);
            }

        } else {

            // Set query properties
            Map<String, String> params = new HashMap<>();
            params.put("prop", "categories");
            params.put("cllimit", "max");
            params.put("titles", String.join("|", fullTitles));
            if (clshow != null) {
                params.put("clshow", clshow);
            }
            if (continueMap != null) {
                params.putAll(continueMap);
            }
            final JsonObject result = request(apiUrl, params);

            try {
                final JsonObject query = result.getJsonObject("query");
                if (query != null) {
                    final JsonObject pages = query.getJsonObject("pages");
                    if (pages != null) {
                        for (String pagesKey : pages.keySet()) {
                            final JsonObject pagesData = pages.getJsonObject(pagesKey);
                            if (pagesData.containsKey("categories")) {
                                final JsonArray jsonCategories = pagesData.getJsonArray("categories");
                                final String pagesDataTitle = pagesData.getString("title");
                                final Collection<String> categories = categoryMap.computeIfAbsent(pagesDataTitle,
                                        k -> new ArrayList<>(jsonCategories.size()));
                                for (int i = 0; i < jsonCategories.size(); i++) {
                                    JsonObject category = jsonCategories.getJsonObject(i);
                                    categories.add(category.getString("title"));
                                }
                            }
                        }
                    }
                }

                // Try to get continue map, if there is one, make another request
                final var newContinueMap = buildContinueMap(result, "categories");
                if (newContinueMap != null) {
                    requestCategoriesRecursive(apiUrl, fullTitles, categoryMap, newContinueMap, clshow);
                }
            } catch (JsonException e) {
                throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
            }

        }
    }

    @Override
    public List<String> requestCategorymembers(Wiki wiki, String fullTitle) throws ApiException {
        final List<String> categories = new ArrayList<>();
        requestCategorymembersRecursive(wiki.getApiUrl(), fullTitle, categories, null);
        return categories;
    }

    private void requestCategorymembersRecursive(String apiUrl, String fullTitle, List<String> categories,
                                                 @Nullable Map<String, String> continueMap) throws ApiException {

        // Set query properties
        final Map<String, String> params = new HashMap<>();
        params.put("list", "categorymembers");
        params.put("cmlimit", "max");
        params.put("cmtitle", fullTitle);
        params.put("cmtype", "subcat");
        if (continueMap != null) {
            params.putAll(continueMap);
        }
        JsonObject result = request(apiUrl, params);

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
                requestCategorymembersRecursive(apiUrl, fullTitle, categories, newContinueMap);
            }
        } catch (JsonException e) {
            throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
        }

    }

    private void requestLinksBetweenRecursive(final String apiUrl, final List<String> fullTitles, final Collection<Pair<String, String>> links,
                                              @Nullable Map<String, String> continueMap) throws ApiException {

        if (fullTitles.size() > TITLES_MAX) {

            for (List<String> fullTitlesPart : ListUtils.partition(fullTitles, TITLES_MAX)) {
                requestLinksBetweenRecursive(apiUrl, fullTitlesPart, links, continueMap);
            }

        } else {

            // Set query properties
            final Map<String, String> params = new HashMap<>();
            params.put("prop", "links");
            params.put("pllimit", "max");
            final String titlesParam = String.join("|", fullTitles);
            params.put("titles", titlesParam);
            params.put("pltitles", titlesParam);
            if (continueMap != null) {
                params.putAll(continueMap);
            }
            final JsonObject result = request(apiUrl, params);

            try {
                final JsonObject query = result.getJsonObject("query");
                if (query != null) {
                    final JsonObject pages = query.getJsonObject("pages");
                    for (String pagesKey : pages.keySet()) {
                        final JsonObject pagesData = pages.getJsonObject(pagesKey);
                        if (pagesData.containsKey("links")) {
                            JsonArray jsonLinks = pagesData.getJsonArray("links");
                            String pagesDataTitle = pagesData.getString("title");
                            for (int i = 0; i < jsonLinks.size(); i++) {
                                final JsonObject category = jsonLinks.getJsonObject(i);
                                links.add(new MutablePair<>(pagesDataTitle, category.getString("title")));
                            }
                        }
                    }
                }

                // Try to get continue map, if there is one, make another request
                final var newContinueMap = buildContinueMap(result, "categories");
                if (newContinueMap != null) {
                    requestLinksBetweenRecursive(apiUrl, fullTitles, links, newContinueMap);
                }
            } catch (JsonException e) {
                throw new ApiException(Messages.getString("ApiClient.Exception.ParsingJSON"), e);
            }

        }
    }

    @Override
    public Metadata requestMetadata(final Wiki wiki) throws ApiException {

        // Set query properties
        final Map<String, String> params = new HashMap<>();
        params.put("meta", "siteinfo");
        params.put("siprop", "general|namespaces|namespacealiases");

        final JsonObject json = request(wiki.getApiUrl(), params);

        final String articlepath;
        final String server;
        final Map<Integer, String> authoritativeNamespaces = new HashMap<>();
        final Map<String, Integer> allNamespacesInverse = new HashMap<>();

        try {
            final JsonObject query = json.getJsonObject("query");

            articlepath = query.getJsonObject("general").getString("articlepath");

            server = query.getJsonObject("general").getString("server");

            JsonObject namespaces = query.getJsonObject("namespaces");
            for (String nsIdString : namespaces.keySet()) {
                final JsonObject namespaceData = namespaces.getJsonObject(nsIdString);
                final int nsId = Integer.parseInt(nsIdString);
                final String nsName = namespaceData.getString("*");
                authoritativeNamespaces.put(nsId, nsName);
                allNamespacesInverse.put(nsName, nsId);
                if (namespaceData.containsKey("canonical")) {
                    final String nsCanonical = namespaceData.getString("canonical");
                    if (nsCanonical != null && !nsCanonical.isEmpty()) {
                        allNamespacesInverse.put(nsCanonical, nsId);
                    }
                }
            }

            final JsonArray namespacealiases = query.getJsonArray("namespacealiases");
            for (int i = 0; i < namespacealiases.size(); i++) {
                final JsonObject namespacealiasData = namespacealiases.getJsonObject(i);
                final int nsId = namespacealiasData.getInt("id");
                final String nsName = namespacealiasData.getString("*");
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
