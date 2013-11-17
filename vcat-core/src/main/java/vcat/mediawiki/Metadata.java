package vcat.mediawiki;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Metadata implements Serializable {

	private static final long serialVersionUID = -4021889274400016225L;

	/** ID of the article namespace. */
	public final static int NamespaceArticle = 0;

	/** ID of the category namespace. */
	public final static int NamespaceCategory = 14;

	/** A map of all namespace names of the MediaWiki installation. */
	private final Map<String, Integer> allNamespacesInverse;

	protected String articlepath;

	/** A map of the authoritative namespace names of the MediaWiki installation. */
	private final Map<Integer, String> authoritativeNamespaces;

	protected Metadata(final IWiki wiki, final String articlepath, final Map<Integer, String> authoritativeNamespaces,
			final Map<String, Integer> allNamespacesInverse) throws ApiException {
		this.articlepath = articlepath;
		this.authoritativeNamespaces = authoritativeNamespaces;
		this.allNamespacesInverse = allNamespacesInverse;
	}

	public String fullTitle(final String title, final int namespace) throws ApiException {
		String namespaceName = this.getAuthoritativeName(namespace);
		if (namespaceName == null) {
			throw new ApiException("Error while determining full article title: unknown namespace " + namespace);
		} else if (namespaceName.isEmpty()) {
			return title;
		} else {
			return namespaceName + ':' + title;
		}
	}

	public Set<String> getAllNames(int namespace) {
		Set<String> allNames = new HashSet<String>();
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

	public String truncateTitle(String fullTitle) throws ApiException {
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
