package vcat.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

public class HashHelperTest {

	private static class TestClass implements Serializable {

		private static final long serialVersionUID = 4591638198169811763L;

	}

	@Test
	public void testSha256Hex() {

		Serializable testObject1 = new TestClass();
		Serializable testObject2 = new TestClass();

		// the result should be the same if the objects are equal
		String result1 = HashHelper.sha256Hex(testObject1);
		String result2 = HashHelper.sha256Hex(testObject2);
		assertEquals(result2, result1);

	}

	@Test
	public void testSha256HexString() {

		String testString = "abcäöü";
		// expected digest is known
		String expectedResult = DigestUtils.sha256Hex(testString.getBytes(StandardCharsets.UTF_8));

		String result = HashHelper.sha256Hex(testString);

		assertEquals(expectedResult, result);

	}

}
