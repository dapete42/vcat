package vcat.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vcat.graph.Node;
import vcat.mediawiki.Metadata;
import vcat.params.Links;
import vcat.test.TestAllParams;
import vcat.test.TestLinkProvider;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractLinkProviderTest {

    private TestLinkProvider underTest;

    @BeforeEach
    public void setUp() {
        underTest = new TestLinkProvider();
    }

    @Test
    public void testEscapeForUrl() {
        assertEquals("abc%3A%C3%84%C3%B6%C3%BC_%C3%9F%E3%83%A1%E3%82%A4%E3%83%B3%E3%83%9A%E3%83%BC%E3%82%B8",
                AbstractLinkProvider.escapeForUrl("abc:Äöü ßメインページ"));
    }

    @Test
    public void testEscapeForUrlNull() {
        assertNull(AbstractLinkProvider.escapeForUrl(null));
    }

    @Test
    public void testEscapeMediawikiTitleForUrl() {
        assertEquals("abc:%C3%84%C3%B6%C3%BC_%C3%9F%E3%83%A1%E3%82%A4%E3%83%B3%E3%83%9A%E3%83%BC%E3%82%B8",
                AbstractLinkProvider.escapeMediawikiTitleForUrl("abc:Äöü ßメインページ"));
    }

    @Test
    public void testEscapeMediawikiTitleForUrlNull() {
        assertNull(AbstractLinkProvider.escapeMediawikiTitleForUrl(null));
    }

    @Test
    public void testFromParamsDefault() {

        TestAllParams params = new TestAllParams();
        params.getVCat().setLinks(Links.None);

        AbstractLinkProvider instance = AbstractLinkProvider.fromParams(params);

        assertTrue(instance instanceof EmptyLinkProvider);

    }

    @Test
    public void testAddLinkToNode() {

        Node node = new Node("test");

        underTest.addLinkToNode(node, "linktitle");

        assertEquals("link:linktitle", node.getHref());

    }

    @Test
    public void testFromParamsGraph() {

        TestAllParams params = new TestAllParams();
        params.getVCat().setLinks(Links.Graph);

        AbstractLinkProvider instance = AbstractLinkProvider.fromParams(params);

        assertTrue(instance instanceof VCatLinkProvider);

    }

    @Test
    public void testFromParamsWiki() {

        TestAllParams params = new TestAllParams();
        Metadata metadata = new Metadata("articlepath", "server", Collections.emptyMap(), Collections.emptyMap());
        params.setMetadata(metadata);
        params.getVCat().setLinks(Links.Wiki);

        AbstractLinkProvider instance = AbstractLinkProvider.fromParams(params);

        assertTrue(instance instanceof WikiLinkProvider);

    }

}
