package vcat.mediawiki;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Metadata implements Serializable {

	protected class General implements Serializable {

		private static final long serialVersionUID = 3451289507538778447L;

		protected String articlepath;

	}

	private static final long serialVersionUID = -285281559588857239L;

	/** ID of the article namespace. */
	public final static int NS_ARTICLE = 0;

	/** ID of the category namespace. */
	public final static int NS_CATEGORY = 14;

	/** A map of the authoritative namespace names of the MediaWiki installation. */
	private final Map<Integer, String> authoritativeNamespaces = new HashMap<Integer, String>();

	/** A map of all namespace names of the MediaWiki installation. */
	private final Map<String, Integer> allNamespacesInverse = new HashMap<String, Integer>();

	private final General general = new General();

	public Metadata(MediawikiApiClient apiClient) throws ApiException {
		apiClient.requestMetadata(this.general, this.authoritativeNamespaces, this.allNamespacesInverse);
	}

	public String fullTitle(String title, int namespace) throws ApiException {
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
		return this.general.articlepath;
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
