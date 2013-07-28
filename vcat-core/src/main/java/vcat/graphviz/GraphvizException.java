package vcat.graphviz;

public class GraphvizException extends Exception {

	private static final long serialVersionUID = 5257306842544515205L;

	public GraphvizException(String message) {
		super(message);
	}

	public GraphvizException(String message, Throwable cause) {
		super(message, cause);
	}

	public GraphvizException(Throwable cause) {
		super(cause);
	}

}
