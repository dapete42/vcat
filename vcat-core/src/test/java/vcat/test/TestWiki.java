package vcat.test;

import vcat.mediawiki.interfaces.Wiki;

public class TestWiki implements Wiki {

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