package vcat.params;

/**
 * Link parameter vor vCat. Determines whether links are included in the graph, and where they should lead to.
 * 
 * @author Peter Schlömer
 */
public enum Link {

	/** No links in graph. */
	None,
	// TODO Links to vCat graphs do not work yet.
	// /** Include links to another vCat graph, starting from the node. */
	// Graph,
	/** Include links to wiki article for node. */
	Wiki;

	public static Link valueOfIgnoreCase(String name) {
		for (Link link : values()) {
			if (link.name().equalsIgnoreCase(name)) {
				return link;
			}
		}
		return null;
	}

}
