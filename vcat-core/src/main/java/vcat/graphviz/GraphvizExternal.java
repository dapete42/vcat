package vcat.graphviz;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import vcat.Messages;
import vcat.params.GraphvizParams;

/**
 * Graphviz renderer which uses the Graphviz command line tools (dot, fdp etc.).
 * 
 * @author Peter Schlömer
 */
public class GraphvizExternal implements Graphviz {

	/** Log4j2 Logger */
	private static final Logger LOGGER = LogManager.getLogger();

	private final File programPath;

	public GraphvizExternal(File programPath) {
		if (!programPath.exists() || !programPath.isDirectory() || !programPath.canRead()) {
			throw new InvalidParameterException(String.format(
					Messages.getString("GraphvizExternal.Exception.ProgramPath"), programPath.getAbsolutePath()));
		}
		this.programPath = programPath;
	}

	public List<String> buildCommandParts(final String command, GraphvizParams params, File inputFile,
			File outputFile) {
		ArrayList<String> commandParts = new ArrayList<>(4);
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
			throw new InvalidParameterException(String.format(
					Messages.getString("GraphvizExternal.Exception.ProgramFile"), programFile.getAbsolutePath()));
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
			throw new GraphvizException(
					String.format(Messages.getString("GraphvizExternal.Exception.Running"), command), e);
		}
		try {
			// Close stdin
			graphvizProcess.getOutputStream().close();
		} catch (IOException e) {
			throw new GraphvizException(String.format(Messages.getString("GraphvizExternal.Exception.Stdin"), command),
					e);
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
			LOGGER.info(String.format(Messages.getString("GraphvizExternal.Info.Finished"), inputFile.getAbsolutePath(),
					endMillis - startMillis));
		} else {
			LOGGER.error(String.format(Messages.getString("GraphvizExternal.Error.ExitCode"),
					inputFile.getAbsolutePath(), exitValue));
			outputFile.delete();
			throw new GraphvizException(
					String.format(Messages.getString("GraphvizExternal.Exception.ExitCode"), exitValue));
		}
	}

}
