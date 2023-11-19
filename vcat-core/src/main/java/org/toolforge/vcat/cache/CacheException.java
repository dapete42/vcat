package org.toolforge.vcat.cache;

import lombok.experimental.StandardException;

import java.io.Serial;

/**
 * Exception to be used by classes in the {@code vcat.cache} package.
 *
 * @author Peter Schlömer
 */
@StandardException
public class CacheException extends Exception {

    @Serial
    private static final long serialVersionUID = -6480864966068661264L;

}
