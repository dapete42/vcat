package vcat.graphviz;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.params.GraphvizParams;

/**
 * Graphviz renderer which uses the Graphviz command line tools (dot, fdp etc.).
 * 
 * @author Peter Schlömer
 */
public class GraphvizExternal implements Graphviz {

	private final Log log = LogFactory.getLog(this.getClass());

	private final File programPath;

	public GraphvizExternal(File programPath) {
		if (!programPath.exists() || !programPath.isDirectory() || !programPath.canRead()) {
			throw new InvalidParameterException("Program path '" + programPath.getAbsolutePath()
					+ "' must exist, be a directory and be readable.");
		}
		this.programPath = programPath;
		if (!this.programPath.exists() || !this.programPath.isDirectory() || !this.programPath.canRead()) {
			throw new InvalidParameterException("Program path '" + this.programPath.getAbsolutePath()
					+ "' must exist, be a directory and be readable.");
		}
	}

	public List<String> buildCommandParts(final String command, GraphvizParams params, File inputFile, File outputFile) {
		ArrayList<String> commandParts = new ArrayList<String>(4);
		commandParts.add(command);
		commandParts.add("-T" + params.getOutputFormat().getGraphvizTypeParameter());
		commandParts.add("-o" + outputFile.getAbsolutePath());
		commandParts.add(inputFile.getAbsolutePath());
		return commandParts;
	}

	@Override
	public void render(File inputFile, File outputFile, GraphvizParams params) throws GraphvizException {
		final Runtime runtime = Runtime.getRuntime();

		final long startMillis = System.currentTimeMillis();

		final File programFile = new File(programPath, params.getAlgorithm().getProgram());
		if (!programFile.exists() || !programFile.canExecute()) {
			throw new InvalidParameterException("Program file '" + programFile.getAbsolutePath()
					+ "' must exist and be executable. Is Graphviz installed?");
		}

		final String command = programFile.getAbsolutePath();

		final List<String> commandList = buildCommandParts(command, params, inputFile, outputFile);
		final int len = commandList.size();
		final String[] commandArray = new String[len];
		for (int i = 0; i < len; i++) {
			commandArray[i] = commandList.get(i);
		}

		Process graphvizProcess;
		try {
			graphvizProcess = runtime.exec(commandArray);
		} catch (IOException e) {
			throw new GraphvizException("Error running graphviz executable '" + command + "'", e);
		}
		try {
			// Close stdin
			graphvizProcess.getOutputStream().close();
		} catch (IOException e) {
			throw new GraphvizException("Error running graphviz: cannot close program stdin", e);
		}
		boolean running = true;
		int exitValue = 0;
		do {
			try {
				exitValue = graphvizProcess.exitValue();
				running = false;
			} catch (IllegalThreadStateException e) {
				// still running
				try {
					Thread.sleep(10);
				} catch (InterruptedException ee) {
					// ignore
				}
			}
		} while (running);

		long endMillis = System.currentTimeMillis();

		if (exitValue == 0) {
			log.info("Graphviz run on input file '" + inputFile.getAbsolutePath() + "'. Total run time: "
					+ (endMillis - startMillis) + " ms.");
		} else {
			log.error("Graphviz run on input file '" + inputFile.getAbsolutePath() + "' failed with exit code "
					+ exitValue + ".");
			outputFile.delete();
			throw new GraphvizException("Error running graphviz: returned exit code " + exitValue);
		}
	}

}
