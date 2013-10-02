package vcat.mediawiki;

public class SimpleWikimediaWiki implements IWiki {

	private static final long serialVersionUID = -8656795250098981777L;

	private final String host;

	public SimpleWikimediaWiki(String host) {
		this.host = host;
	}

	@Override
	public String getApiUrl() {
		return "http://" + this.host + "/w/api.php";
	}

	@Override
	public String getDisplayName() {
		return this.host;
	}

	@Override
	public String getName() {
		return this.host;
	}

}
