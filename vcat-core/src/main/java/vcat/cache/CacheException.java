package vcat.cache;

/**
 * Exception to be used by classes in the {@code vcat.cache} package.
 * 
 * @author Peter Schlömer
 */
public class CacheException extends Exception {

	private static final long serialVersionUID = -5629241518904562787L;

	public CacheException(String message) {
		super(message);
	}

	public CacheException(String message, Throwable cause) {
		super(message, cause);
	}

}
