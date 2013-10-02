package vcat.mediawiki;

public class SimpleWiki implements IWiki {

	private static final long serialVersionUID = 1825175390771086841L;

	private final String apiUrl;

	private final String name;

	public SimpleWiki(final String name, final String apiUrl) {
		this.name = name;
		this.apiUrl = apiUrl;
	}

	@Override
	public String getApiUrl() {
		return this.apiUrl;
	}

	@Override
	public String getDisplayName() {
		return this.name;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
