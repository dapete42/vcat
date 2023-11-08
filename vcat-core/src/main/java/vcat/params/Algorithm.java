package vcat.params;

/**
 * Algorithm for vCat.
 *
 * @author Peter Schlömer
 */
public enum Algorithm {

    /**
     * dot draws directed graphs. It works well on DAGs and other graphs that can be drawn as hierarchies. (Text from
     * Graphviz man page)
     */
    DOT("dot"),
    /**
     * fdp draws undirected graphs using a "spring" model. (Text from Graphviz man page)
     */
    FDP("fdp");

    public static Algorithm valueOfIgnoreCase(String name) {
        for (Algorithm format : values()) {
            if (format.name().equalsIgnoreCase(name)) {
                return format;
            }
        }
        return null;
    }

    private String program;

    Algorithm(String program) {
        this.program = program;
    }

    /**
     * @return graphviz program to use when rendering with this algorithm.
     */
    public String getProgram() {
        return this.program;
    }

}
