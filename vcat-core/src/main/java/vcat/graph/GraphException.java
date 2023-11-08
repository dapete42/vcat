package vcat.graph;

public class GraphException extends Exception {

    private static final long serialVersionUID = 381013055055800767L;

    public GraphException(String message) {
        super(message);
    }

    public GraphException(String message, Throwable cause) {
        super(message, cause);
    }

}
