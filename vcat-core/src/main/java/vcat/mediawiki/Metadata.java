package vcat.mediawiki;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.helpers.MessageFormatter;

import java.util.Set;

import vcat.Messages;

public class Metadata implements Serializable {

	private static final long serialVersionUID = -2562390668664997668L;

	/** ID of the article namespace. */
	public static final int NS_ARTICLE = 0;

	/** ID of the category namespace. */
	public static final int NS_CATEGORY = 14;

	/** A map of all namespace names of the MediaWiki installation. */
	private final Map<String, Integer> allNamespacesInverse;

	/** The path on the server to access wiki pages (contains <code>$1</code> as a placeholder for the page title). */
	protected final String articlepath;

	/** A map of the authoritative namespace names of the MediaWiki installation. */
	private final Map<Integer, String> authoritativeNamespaces;

	/** The server the wiki is running on (start of URL). */
	protected final String server;

	public Metadata(final String articlepath, final String server, final Map<Integer, String> authoritativeNamespaces,
			final Map<String, Integer> allNamespacesInverse) {
		this.articlepath = articlepath;
		this.server = server;
		this.authoritativeNamespaces = authoritativeNamespaces;
		this.allNamespacesInverse = allNamespacesInverse;
	}

	public String fullTitle(final String title, final int namespace) throws ApiException {
		String namespaceName = this.getAuthoritativeName(namespace);
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
		Set<String> allNames = new HashSet<>();
		for (Entry<String, Integer> entry : allNamespacesInverse.entrySet()) {
			if (namespace == entry.getValue()) {
				allNames.add(entry.getKey());
			}
		}
		return allNames;
	}

	public Map<String, Integer> getAllNamespacesInverseMap() {
		return Collections.unmodifiableMap(this.allNamespacesInverse);
	}

	public String getArticlepath() {
		return this.articlepath;
	}

	public String getAuthoritativeName(int namespace) {
		return this.authoritativeNamespaces.get(namespace);
	}

	public int getId(String namespaceName) {
		return this.allNamespacesInverse.get(namespaceName);
	}

	public String getServer() {
		return this.server;
	}

	public int namespaceFromTitle(String title) {
		for (Entry<String, Integer> entry : this.allNamespacesInverse.entrySet()) {
			String key = entry.getKey();
			if (!key.isEmpty()) {
				String prefix = key + ":";
				if (title.startsWith(prefix)) {
					return entry.getValue();
				}
			}
		}
		return 0;
	}

	public String titleWithoutNamespace(String title) {
		for (Entry<String, Integer> entry : this.allNamespacesInverse.entrySet()) {
			String key = entry.getKey();
			if (!key.isEmpty()) {
				String prefix = key + ":";
				if (title.startsWith(prefix)) {
					return title.substring(prefix.length());
				}
			}
		}
		return title;
	}

	public String truncateTitle(String fullTitle) {
		for (Entry<String, Integer> entry : this.allNamespacesInverse.entrySet()) {
			String key = entry.getKey();
			if (!key.isEmpty()) {
				String prefix = key + ":";
				if (fullTitle.startsWith(prefix)) {
					return fullTitle.substring(prefix.length());
				}
			}
		}
		return fullTitle;
	}

}
