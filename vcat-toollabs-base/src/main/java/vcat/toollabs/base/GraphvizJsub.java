package vcat.toollabs.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vcat.graphviz.GraphvizExternal;
import vcat.params.GraphvizParams;

/**
 * Graphviz renderer which uses the jsub script to submit the Graphviz command line tools (dot, fdp etc.) as Grid Engine
 * jobs.
 * 
 * @author Peter Schlömer
 */
public class GraphvizJsub extends GraphvizExternal {

	private final ArrayList<String> jsubCommandParts = new ArrayList<>();

	/** Job memory in Megabytes */
	private final int mem;

	public GraphvizJsub(final File graphvizPath, int mem) {
		super(graphvizPath);
		this.mem = mem;
		this.buildJsubCommandParts();
	}

	private void buildJsubCommandParts() {
		this.jsubCommandParts.clear();
		this.jsubCommandParts.add("jsub");
		// Job name
		this.jsubCommandParts.add("-N");
		this.jsubCommandParts.add("vcat-graphviz");
		// Don't make any job related output
		this.jsubCommandParts.add("-quiet");
		// Memory to use
		this.jsubCommandParts.add("-mem");
		this.jsubCommandParts.add(Integer.toString(this.getMem()) + 'm');
		// Run the job synchronously, so we know when it has finished
		this.jsubCommandParts.add("-sync");
		this.jsubCommandParts.add("y");
	}

	@Override
	public List<String> buildCommandParts(String command, GraphvizParams params, File inputFile, File outputFile) {
		final List<String> originalCommandParts = super.buildCommandParts(command, params, inputFile, outputFile);
		final ArrayList<String> commandParts = new ArrayList<>(this.jsubCommandParts.size()
				+ originalCommandParts.size());
		commandParts.addAll(this.jsubCommandParts);
		commandParts.addAll(originalCommandParts);
		return commandParts;
	}

	/**
	 * Get job memory in Megabytes.
	 * 
	 * @return Job memory in Megabytes
	 */
	public int getMem() {
		return this.mem;
	}

}
