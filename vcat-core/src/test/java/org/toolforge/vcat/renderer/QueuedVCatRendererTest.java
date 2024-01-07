package org.toolforge.vcat.renderer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.test.TestAllParams;
import org.toolforge.vcat.test.TestVCatRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueuedVCatRendererTest {

    private TestVCatRenderer testRenderer;

    private QueuedVCatRenderer underTest;

    @BeforeEach
    public void setUp() {
        testRenderer = new TestVCatRenderer();
        underTest = new QueuedVCatRenderer(testRenderer, 2);
    }

    @Test
    public void testRender() throws VCatException {

        TestAllParams params = new TestAllParams();

        underTest.render(params);

        assertEquals(1, testRenderer.getCreatedGraphFiles().size());
        assertEquals(1, testRenderer.getRenderedFileFromGraphFiles().size());

    }

}
