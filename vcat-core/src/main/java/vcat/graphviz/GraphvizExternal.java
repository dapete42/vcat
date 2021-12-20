package vcat.graphviz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import vcat.Messages;
import vcat.params.GraphvizParams;

/**
 * Graphviz renderer which uses the Graphviz command line tools (dot, fdp etc.).
 * 
 * @author Peter Schlömer
 */
public class GraphvizExternal implements Graphviz {

	/** Log4j2 Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(GraphvizExternal.class);

	private final Path programPath;

	public GraphvizExternal(Path programPath) {
		if (!Files.exists(programPath) || !Files.isDirectory(programPath) || !Files.isReadable(programPath)) {
			throw new InvalidParameterException(MessageFormatter
					.format(Messages.getString("GraphvizExternal.Exception.ProgramPath"), programPath.toAbsolutePath())
					.getMessage());
		}
		this.programPath = programPath;
	}

	public List<String> buildCommandParts(final String command, GraphvizParams params, Path inputFile,
			Path outputFile) {
		ArrayList<String> commandParts = new ArrayList<>(4);
		commandParts.add(command);
		commandParts.add("-T" + params.getOutputFormat().getGraphvizTypeParameter());
		commandParts.add("-o" + outputFile.toAbsolutePath().toString());
		commandParts.add(inputFile.toAbsolutePath().toString());
		return commandParts;
	}

	@Override
	public void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException {
		final Runtime runtime = Runtime.getRuntime();

		final long startMillis = System.currentTimeMillis();

		final Path programFile = programPath.resolve(params.getAlgorithm().getProgram());
		final String command = programFile.toAbsolutePath().toString();
		if (!Files.exists(programFile) || !Files.isExecutable(programFile)) {
			throw new InvalidParameterException(MessageFormatter
					.format(Messages.getString("GraphvizExternal.Exception.ProgramFile"), command).getMessage());
		}

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
			throw new GraphvizException(MessageFormatter
					.format(Messages.getString("GraphvizExternal.Exception.Running"), command).getMessage(), e);
		}
		try {
			// Close stdin
			graphvizProcess.getOutputStream().close();
		} catch (IOException e) {
			throw new GraphvizException(MessageFormatter
					.format(Messages.getString("GraphvizExternal.Exception.Stdin"), command).getMessage(), e);
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
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException ee) {
					Thread.currentThread().interrupt();
				}
			}
		} while (running);

		long endMillis = System.currentTimeMillis();

		if (exitValue == 0) {
			LOGGER.info(Messages.getString("GraphvizExternal.Info.Finished"), inputFile.toAbsolutePath(),
					endMillis - startMillis);
		} else {
			LOGGER.error(Messages.getString("GraphvizExternal.Error.ExitCode"), inputFile.toAbsolutePath(), exitValue);
			try {
				Files.delete(outputFile);
			} catch (IOException e) {
				LOGGER.error("Could not remove output file '{}' after error", outputFile, e);
			}
			throw new GraphvizException(MessageFormatter
					.format(Messages.getString("GraphvizExternal.Exception.ExitCode"), exitValue).getMessage());
		}
	}

}
