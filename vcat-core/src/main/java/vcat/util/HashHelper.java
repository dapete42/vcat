package vcat.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class HashHelper {

    private HashHelper() {
    }

    /**
     * Calculates the SHA-256 digest from a serialized form of a Serializable object and returns the value as a hex
     * string.
     *
     * @param object Serializable object to digest
     * @return SHA-256 digest as a hex string
     */
    public static String sha256Hex(Serializable object) {
        if (object instanceof String) {
            // For strings, directly use bytes
            return DigestUtils.sha256Hex(((String) object).getBytes(StandardCharsets.UTF_8));
        } else {
            return DigestUtils.sha256Hex(SerializationUtils.serialize(object));
        }
    }

}
