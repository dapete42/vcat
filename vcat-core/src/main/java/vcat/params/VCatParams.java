package vcat.params;

import java.io.Serializable;
import java.util.Collection;

import vcat.mediawiki.interfaces.Wiki;
import vcat.util.AbstractLinkProvider;

/**
 * Parameters for the graph generation used by the {@link vcat.AbstractVCat VCat} class.
 * 
 * @author Peter Schlömer
 */
public class VCatParams<W extends Wiki> implements Serializable {

	private static final long serialVersionUID = -7181473469976962467L;

	private Integer depth;

	private Integer limit;

	/** Link provider. Included here because different linking requires different cache entries. */
	private AbstractLinkProvider linkProvider;

	private Links links = Links.None;

	private Relation relation = Relation.Category;

	private boolean showhidden = false;

	private Collection<TitleNamespaceParam> titleNamespaceParams;

	private W wiki;

	public Integer getDepth() {
		return this.depth;
	}

	public Integer getLimit() {
		return this.limit;
	}

	public AbstractLinkProvider getLinkProvider() {
		return this.linkProvider;
	}

	public Links getLinks() {
		return this.links;
	}

	public Relation getRelation() {
		return this.relation;
	}

	public Collection<TitleNamespaceParam> getTitleNamespaceParams() {
		return this.titleNamespaceParams;
	}

	public W getWiki() {
		return this.wiki;
	}

	public boolean isShowhidden() {
		return this.showhidden;
	}

	public void setDepth(Integer depth) {
		this.depth = depth;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public void setLinkProvider(AbstractLinkProvider linkProvider) {
		this.linkProvider = linkProvider;
	}

	public void setLinks(Links links) {
		this.links = links;
	}

	public void setRelation(Relation relation) {
		this.relation = relation;
	}

	public void setShowhidden(boolean showhidden) {
		this.showhidden = showhidden;
	}

	public void setTitleNamespaceParams(Collection<TitleNamespaceParam> titleNamespaceParams) {
		this.titleNamespaceParams = titleNamespaceParams;
	}

	public void setWiki(W wiki) {
		this.wiki = wiki;
	}

}
