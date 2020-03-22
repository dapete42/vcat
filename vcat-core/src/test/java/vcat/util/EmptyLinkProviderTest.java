package vcat.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
