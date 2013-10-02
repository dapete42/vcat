package vcat.params;

import java.io.Serializable;

import vcat.mediawiki.IWiki;

/**
 * Parameters for the graph generation used by the {@link vcat.AbstractVCat VCat} class.
 * 
 * @author Peter Schlömer
 */
public class VCatParams<W extends IWiki> implements Serializable {

	private static final long serialVersionUID = 1489025067225330489L;

	private Integer depth;

	private Integer limit;

	private int namespace = 0;

	private Relation relation = Relation.Category;

	private boolean showhidden = false;

	private String title;

	private W wiki;

	public Integer getDepth() {
		return this.depth;
	}

	public Integer getLimit() {
		return this.limit;
	}

	public int getNamespace() {
		return this.namespace;
	}

	public String getTitle() {
		return this.title;
	}

	public W getWiki() {
		return this.wiki;
	}

	public Relation getRelation() {
		return this.relation;
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

	public void setNamespace(int namespace) {
		this.namespace = namespace;
	}

	public void setRelation(Relation relation) {
		this.relation = relation;
	}

	public void setShowhidden(boolean showhidden) {
		this.showhidden = showhidden;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setWiki(W wiki) {
		this.wiki = wiki;
	}

}
