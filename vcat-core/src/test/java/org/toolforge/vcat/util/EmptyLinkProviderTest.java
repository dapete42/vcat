package org.toolforge.vcat.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class EmptyLinkProviderTest {

    private EmptyLinkProvider underTest;

    @BeforeEach
    public void setUp() {
        underTest = new EmptyLinkProvider();
    }

    @Test
    public void testProviceLink() {

        assertNull(underTest.provideLink("test"));

    }

}
