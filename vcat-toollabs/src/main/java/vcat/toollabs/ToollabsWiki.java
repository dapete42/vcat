package vcat.toollabs;

import vcat.mediawiki.IWiki;

public class ToollabsWiki implements IWiki {

	private static final long serialVersionUID = -8039078925472549881L;

	private final String dbname;

	private final String name;

	private final String url;

	protected ToollabsWiki(final String dbname, final String name, final String url) {
		this.dbname = dbname;
		this.name = name;
		this.url = url;
	}

	@Override
	public String getApiUrl() {
		return this.url + "/w/api.php";
	}

	public String getDbname() {
		return this.dbname;
	}

	@Override
	public String getName() {
		return this.dbname;
	}

	@Override
	public String getDisplayName() {
		return this.name + " (" + this.dbname + ')';
	}

}
