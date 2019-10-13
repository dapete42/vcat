package vcat.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class EmptyLinkProviderTest {

	private EmptyLinkProvider underTest;

	@Before
	public void setUp() {
		underTest = new EmptyLinkProvider();
	}

	@Test
	public void testProviceLink() {

		assertNull(underTest.provideLink("test"));

	}

}
