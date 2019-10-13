package vcat.test;

import vcat.mediawiki.IWiki;

public class TestWiki implements IWiki {

	private static final long serialVersionUID = 6023746140482931907L;

	@Override
	public String getApiUrl() {
		return "http://api.url";
	}

	@Override
	public String getDisplayName() {
		return "Test";
	}

	@Override
	public String getName() {
		return "test";
	}

}