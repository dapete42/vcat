package vcat.util;

import java.io.Serializable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.SerializationUtils;

public abstract class HashHelper {

	/**
	 * Create a hash value from an array of bytes with data. Currently uses SHA-1, converted to a hex string.
	 * 
	 * @param bytes
	 *            Data.
	 * @return A hash value from the data.
	 */
	public static String hashFor(byte[] bytes) {
		return DigestUtils.sha1Hex(bytes);
	}

	/**
	 * Return a hash string for a serializable object.
	 * 
	 * @param object
	 *            Serializable object.
	 * @return Hash string for serializable object.
	 */
	public static String hashFor(Serializable object) {
		if (object instanceof String) {
			// For strings, directly use bytes
			return hashFor(((String) object).getBytes());
		} else {
			return hashFor(SerializationUtils.serialize(object));
		}
	}

}
