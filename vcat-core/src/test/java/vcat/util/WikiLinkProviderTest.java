package vcat.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import vcat.mediawiki.Metadata;
import vcat.test.TestAllParams;

public class WikiLinkProviderTest {

	@Test
	public void testProvideLink() {

		TestAllParams params = new TestAllParams();
		Metadata metadata = new Metadata("articlepath/$1", "https://server/", Collections.emptyMap(),
				Collections.emptyMap());
		params.setMetadata(metadata);
		WikiLinkProvider underTest = new WikiLinkProvider(params);

		assertEquals(
				"https://server/articlepath/abc:%C3%84%C3%B6%C3%BC_%C3%9F%E3%83%A1%E3%82%A4%E3%83%B3%E3%83%9A%E3%83%BC%E3%82%B8",
				underTest.provideLink("abc:Äöü ßメインページ"));

	}

	@Test
	public void testProvideLinkProtocolRelative() {

		TestAllParams params = new TestAllParams();
		Metadata metadata = new Metadata("articlepath/$1", "//server/", Collections.emptyMap(), Collections.emptyMap());
		params.setMetadata(metadata);
		WikiLinkProvider underTest = new WikiLinkProvider(params);

		assertEquals(
				"http://server/articlepath/abc:%C3%84%C3%B6%C3%BC_%C3%9F%E3%83%A1%E3%82%A4%E3%83%B3%E3%83%9A%E3%83%BC%E3%82%B8",
				underTest.provideLink("abc:Äöü ßメインページ"));

	}

}
