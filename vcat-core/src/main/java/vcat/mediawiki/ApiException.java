package vcat.mediawiki;

public class ApiException extends Exception {

	private static final long serialVersionUID = 3171272212897847189L;

	public ApiException(String message) {
		super(message);
	}

	public ApiException(String message, Throwable cause) {
		super(message, cause);
	}

}
