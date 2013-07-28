package vcat.mediawiki;

public class WikimediaWiki implements IWiki {

	private static final long serialVersionUID = 7957895543237340209L;
	
	private final String host;

	public WikimediaWiki(String host) {
		this.host = host;
	}

	@Override
	public String getApiUrl() {
		return "http://" + this.host + "/w/api.php";
	}

	@Override
	public String getName() {
		return this.host;
	}

}
