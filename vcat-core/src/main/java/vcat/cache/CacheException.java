package vcat.cache;

/**
 * Exception to be used by classes in the {@code vcat.cache} package.
 * 
 * @author Peter Schlömer
 */
public class CacheException extends Exception {

	private static final long serialVersionUID = -4820106025233905079L;

	public CacheException(Throwable cause) {
		super(cause);
	}

	public CacheException(String message) {
		super(message);
	}

	public CacheException(String message, Throwable cause) {
		super(message, cause);
	}

}
