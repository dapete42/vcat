package vcat;

public class VCatException extends Exception {

	private static final long serialVersionUID = -8424059815740621682L;

	public VCatException(String message) {
		super(message);
	}

	public VCatException(String message, Throwable cause) {
		super(message, cause);
	}

	public VCatException(Throwable cause) {
		super(cause);
	}

}
