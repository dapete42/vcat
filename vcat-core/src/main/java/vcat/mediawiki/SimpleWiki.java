package vcat.mediawiki;

public class SimpleWiki implements IWiki {

	private static final long serialVersionUID = 5272016600227754307L;

	private final String apiUrl;

	private final String name;

	public SimpleWiki(String name, String apiUrl) {
		this.name = name;
		this.apiUrl = apiUrl;
	}

	@Override
	public String getApiUrl() {
		return this.apiUrl;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
