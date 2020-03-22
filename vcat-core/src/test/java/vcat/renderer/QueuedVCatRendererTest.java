package vcat.renderer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vcat.VCatException;
import vcat.test.TestAllParams;
import vcat.test.TestVCatRenderer;
import vcat.test.TestWiki;

public class QueuedVCatRendererTest {

	private TestVCatRenderer testRenderer;

	private QueuedVCatRenderer<TestWiki> underTest;

	@BeforeEach
	public void setUp() throws Exception {
		testRenderer = new TestVCatRenderer();
		underTest = new QueuedVCatRenderer<>(testRenderer, 2);
	}

	@Test
	public void testRender() throws VCatException {

		TestAllParams params = new TestAllParams();

		underTest.render(params);

		assertEquals(1, testRenderer.getCreatedGraphFiles().size());
		assertEquals(1, testRenderer.getRenderedFileFromGraphFiles().size());

	}

	@Test
	public void testRenderConcurrent() throws VCatException {

		// Use an artifical delay in the test renderer and try to render 100 times at the same time.
		// testRenderer.createGraphFile(...) should only be called once.

		testRenderer.setDelay(1000);
		TestAllParams params = new TestAllParams();

		for (int i = 0; i < 99; i++) {
			new Thread(() -> {
				try {
					underTest.render(params);
					assertEquals(1, testRenderer.getCreatedGraphFiles().size());
					assertEquals(1, testRenderer.getRenderedFileFromGraphFiles().size());
				} catch (VCatException e) {
					throw new RuntimeException(e);
				}
			}).start();
		}
		underTest.render(params);

		assertEquals(1, testRenderer.getCreatedGraphFiles().size());
		assertEquals(1, testRenderer.getRenderedFileFromGraphFiles().size());

	}

}
