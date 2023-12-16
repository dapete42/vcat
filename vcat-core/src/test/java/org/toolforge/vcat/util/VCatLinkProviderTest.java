package org.toolforge.vcat.util;

import org.junit.jupiter.api.Test;
import org.toolforge.vcat.test.TestAllParams;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        params.putRequestParam("category", List.of("category"));
        params.putRequestParam("ns", List.of("ns"));
        params.putRequestParam("title", List.of("title"));
        params.putRequestParam("a", List.of("1"));
        params.putRequestParam("b", List.of("2", "3"));

        VCatLinkProvider underTest = new VCatLinkProvider(params, "https://server/vcat");

        assertEquals("https://server/vcat?title="
                + "abc:%C3%84%C3%B6%C3%BC_%C3%9F%E3%83%A1%E3%82%A4%E3%83%B3%E3%83%9A%E3%83%BC%E3%82%B8"
                + "&amp;a=1&amp;b=2&amp;b=3", underTest.provideLink("abc:Äöü ßメインページ"));

    }

}
