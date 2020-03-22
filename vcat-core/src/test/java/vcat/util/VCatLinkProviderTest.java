package vcat.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import vcat.test.TestAllParams;

public class VCatLinkProviderTest {

	@Test
	public void testGetRenderUrl() {
		String renderUrl = "https://server/vcat";
		TestAllParams params = new TestAllParams();
		VCatLinkProvider underTest = new VCatLinkProvider(params, renderUrl);
		assertEquals(renderUrl, underTest.getRenderUrl());
	}

	@Test
	public void testProvideLink() {

		TestAllParams params = new TestAllParams();
		params.putRequestParam("category", new String[] { "category" });
		params.putRequestParam("ns", new String[] { "ns" });
		params.putRequestParam("title", new String[] { "title" });
		params.putRequestParam("a", new String[] { "1" });
		params.putRequestParam("b", new String[] { "2", "3" });
		params.putRequestParam("c", new String[] { "4", null });

		VCatLinkProvider underTest = new VCatLinkProvider(params, "https://server/vcat");

		assertEquals("https://server/vcat?title="
				+ "abc:%C3%84%C3%B6%C3%BC_%C3%9F%E3%83%A1%E3%82%A4%E3%83%B3%E3%83%9A%E3%83%BC%E3%82%B8"
				+ "&amp;a=1&amp;b=2&amp;b=3&amp;c=4&amp;c", underTest.provideLink("abc:Äöü ßメインページ"));

	}

}
