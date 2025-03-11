package org.toolforge.vcat.mediawiki;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.slf4j.helpers.MessageFormatter;
import org.toolforge.vcat.Messages;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Metadata implements Serializable {

    @Serial
    private static final long serialVersionUID = -2562390668664997668L;

    /**
     * ID of the article namespace.
     */
    public static final int NS_ARTICLE = 0;

    /**
     * ID of the category namespace.
     */
    public static final int NS_CATEGORY = 14;

    /**
     * A map of all namespace names of the MediaWiki installation.
     */
    private final Map<String, Integer> allNamespacesInverse;

    /**
     * The path on the server to access wiki pages (contains <code>$1</code> as a placeholder for the page title).
     */
    @Getter
    protected final String articlepath;

    /**
     * A map of the authoritative namespace names of the MediaWiki installation.
     */
    private final Map<Integer, String> authoritativeNamespaces;

    /**
     * The server the wiki is running on (start of URL).
     */
    @Getter
    protected final String server;

    public Metadata(String articlepath, String server, Map<Integer, String> authoritativeNamespaces, Map<String, Integer> allNamespacesInverse) {
        this.articlepath = articlepath;
        this.server = server;
        this.authoritativeNamespaces = authoritativeNamespaces;
        this.allNamespacesInverse = allNamespacesInverse;
    }

    public String fullTitle(String title, int namespace) throws ApiException {
        final String namespaceName = getAuthoritativeName(namespace);
        if (namespaceName == null) {
            throw new ApiException(MessageFormatter
                    .format(Messages.getString("Metadata.Exception.ArticleTitle"), namespace).getMessage());
        } else if (namespaceName.isEmpty()) {
            return title;
        } else {
            return namespaceName + ':' + title;
        }
    }

    public Set<String> getAllNames(int namespace) {
        final Set<String> allNames = new HashSet<>();
        for (Entry<String, Integer> entry : allNamespacesInverse.entrySet()) {
            if (namespace == entry.getValue()) {
                allNames.add(entry.getKey());
            }
        }
        return allNames;
    }

    public Map<String, Integer> getAllNamespacesInverse() {
        return Collections.unmodifiableMap(allNamespacesInverse);
    }

    @Nullable
    public String getAuthoritativeName(int namespace) {
        return authoritativeNamespaces.get(namespace);
    }

    public int namespaceFromTitle(String title) {
        for (Entry<String, Integer> entry : allNamespacesInverse.entrySet()) {
            final String key = entry.getKey();
            if (!key.isEmpty()) {
                final String prefix = key + ":";
                if (title.startsWith(prefix)) {
                    return entry.getValue();
                }
            }
        }
        return NS_ARTICLE;
    }

    public String titleWithoutNamespace(String title) {
        for (Entry<String, Integer> entry : allNamespacesInverse.entrySet()) {
            final String key = entry.getKey();
            if (!key.isEmpty()) {
                final String prefix = key + ":";
                if (title.startsWith(prefix)) {
                    return title.substring(prefix.length());
                }
            }
        }
        return title;
    }

}
