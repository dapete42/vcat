package vcat.test;

import vcat.util.AbstractLinkProvider;

public class TestLinkProvider extends AbstractLinkProvider {

	private static final long serialVersionUID = 4438603179157010925L;

	@Override
	public String provideLink(String title) {
		return "link:" + title;
	}

}