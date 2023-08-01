package vcat.graphviz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import vcat.Messages;
import vcat.params.GraphvizParams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Graphviz renderer which uses the Graphviz command line tools (dot, fdp etc.).
 *
 * @author Peter Schlömer
 */
public class GraphvizExternal implements Graphviz {

    /**
     * Log4j2 Logger
     */
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
        return List.of(
                command,
                "-T" + params.getOutputFormat().getGraphvizTypeParameter(),
                "-o" + outputFile.toAbsolutePath(),
                inputFile.toAbsolutePath().toString()
        );
    }

    @Override
    public void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException {
        final long startMillis = System.currentTimeMillis();

        final Path programFile = programPath.resolve(params.getAlgorithm().getProgram());
        final String command = programFile.toAbsolutePath().toString();
        if (!Files.exists(programFile) || !Files.isExecutable(programFile)) {
            throw new InvalidParameterException(MessageFormatter
                    .format(Messages.getString("GraphvizExternal.Exception.ProgramFile"), command).getMessage());
        }

        final List<String> commandList = buildCommandParts(command, params, inputFile, outputFile);
        final String[] commandArray = commandList.toArray(String[]::new);

        final ProcessBuilder processBuilder = new ProcessBuilder(commandArray)
                .redirectErrorStream(true);
        final Process graphvizProcess;
        try {
            graphvizProcess = processBuilder.start();
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
        String processOutput = null;
        try {
            processOutput = new String(graphvizProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // If this fails, just log an error
            LOGGER.warn(Messages.getString("GraphvizExternal.Warn.Stdout"), command, e);
        }
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
                LOGGER.error(Messages.getString("GraphvizExternal.Error.CouldNotRemoveOutputFile"), outputFile, e);
            }
            if (processOutput != null) {
                LOGGER.error(Messages.getString("GraphvitExternal.Error.ProcessOutput"), processOutput);
            }
            throw new GraphvizException(MessageFormatter
                    .format(Messages.getString("GraphvizExternal.Exception.ExitCode"), exitValue).getMessage());
        }
    }

}
