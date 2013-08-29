package vcat.toollabs;

import vcat.mediawiki.IWiki;

public class ToollabsWiki implements IWiki {

	private static final long serialVersionUID = 6128456466437536067L;

	private final String dbname;

	private final String lang;

	private final String name;

	private final String url;

	public ToollabsWiki(final String dbname, final String lang, final String name, final String url) {
		this.dbname = dbname;
		this.lang = lang;
		this.name = name;
		this.url = url;
	}

	@Override
	public String getApiUrl() {
		return this.url + "/w/api.php";
	}

	@Override
	public String getName() {
		return this.name + " (" + this.dbname + ')';
	}

}
