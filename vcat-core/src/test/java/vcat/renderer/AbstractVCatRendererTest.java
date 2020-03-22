package vcat.renderer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vcat.VCatException;
import vcat.params.Links;
import vcat.params.OutputFormat;
import vcat.test.TestAllParams;
import vcat.test.TestVCatRenderer;

public class AbstractVCatRendererTest {

	private TestVCatRenderer underTest;

	@BeforeEach
	public void setUp() throws Exception {
		underTest = new TestVCatRenderer();
	}

	@Test
	public void testRender() throws VCatException {

		TestAllParams params = new TestAllParams();

		underTest.render(params);

		assertEquals(1, underTest.getCreatedGraphFiles().size());
		assertEquals(0, underTest.getCreatedImagemapHtmlFiles().size());
		assertEquals(1, underTest.getRenderedFileFromGraphFiles().size());

	}

	@Test
	public void testRenderLinks() throws VCatException {

		// SVG has links directly included
		TestAllParams params = new TestAllParams();
		params.getGraphviz().setOutputFormat(OutputFormat.SVG);
		params.getVCat().setLinks(Links.Wiki);

		underTest.render(params);

		assertEquals(1, underTest.getCreatedGraphFiles().size());
		assertEquals(0, underTest.getCreatedImagemapHtmlFiles().size());
		assertEquals(1, underTest.getRenderedFileFromGraphFiles().size());

	}

	@Test
	public void testRenderLinksImageMap() throws VCatException {

		// PNG is embedded in an HTML page to enable links
		TestAllParams params = new TestAllParams();
		params.getGraphviz().setOutputFormat(OutputFormat.PNG);
		params.getVCat().setLinks(Links.Wiki);

		underTest.render(params);

		assertEquals(0, underTest.getCreatedGraphFiles().size());
		assertEquals(1, underTest.getCreatedImagemapHtmlFiles().size());
		assertEquals(0, underTest.getRenderedFileFromGraphFiles().size());

	}

	@Test
	public void testRenderRaw() throws VCatException {

		TestAllParams params = new TestAllParams();
		params.getGraphviz().setOutputFormat(OutputFormat.GraphvizRaw);

		underTest.render(params);

		assertEquals(1, underTest.getCreatedGraphFiles().size());
		assertEquals(0, underTest.getCreatedImagemapHtmlFiles().size());
		assertEquals(0, underTest.getRenderedFileFromGraphFiles().size());

	}

}
