package vcat.toollabs.webapp;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import org.apache.commons.lang3.RandomStringUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.params.GraphvizParams;
import vcat.redis.SimplePubSub;

public class GraphvizGridClient implements Graphviz {

	private class ExecStatus {

		boolean aborted = false;

		boolean finished = false;

	}

	private static int runtimeExec(String... commandArray) throws IOException {
		final Runtime runtime = Runtime.getRuntime();

		final Process graphvizProcess = runtime.exec(commandArray);
		// Close stdin for the process
		graphvizProcess.getOutputStream().close();

		do {
			try {
				return graphvizProcess.exitValue();
			} catch (IllegalThreadStateException e) {
				// still running
				try {
					Thread.sleep(10);
				} catch (InterruptedException ee) {
					// ignore
				}
			}
		} while (true);

	}

	private final JedisPool jedisPool;

	private final String redisSecret;

	private final File scriptPath;

	private final File programPath;

	public GraphvizGridClient(final JedisPool jedisPool, final String redisSecret, final File scriptPath,
			final File programPath) {
		this.jedisPool = jedisPool;
		this.redisSecret = redisSecret;
		this.scriptPath = scriptPath;
		this.programPath = programPath;
	}

	private void exec(String... cmdarray) throws GraphvizException {
		// Message to be sent to Redis
		final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
		for (String cmd : cmdarray) {
			jsonArrayBuilder.add(cmd);
		}
		final String message = jsonArrayBuilder.build().toString();

		final String requestChannel = this.redisSecret + "-request";
		final String responseChannel = this.redisSecret + "-response";
		final String requestPrefix = this.redisSecret + "-request-";

		// Random ID for the request
		final String id = RandomStringUtils.randomNumeric(16);

		{
			final Jedis jedis = this.jedisPool.getResource();

			// Put message in Redis
			jedis.set(requestPrefix + id, message);
			// Publish id to Redis request channel
			final long listening = jedis.publish(requestChannel, id);

			if (listening == 0) {
				// Nobody is listening, so we spawn a new instance of the gridserver, telling it to immediately run the
				// command we just submitted. We assume it will run and return when it's finished.
				try {
					runtimeExec(new File(scriptPath, "gridserverStart").toString(), id);
				} catch (IOException e) {
					throw new GraphvizException("Nobody listening, starting gridserver for id %s", e);
				}
			}

			// Status of execution
			final ExecStatus execStatus = new ExecStatus();

			// Timer for aborting
			final Timer abortTimer = new Timer();

			// PubSub to listen for Redis messages
			final SimplePubSub jedisSubscribe = new SimplePubSub() {

				@Override
				public void onMessage(final String channel, final String message) {
					synchronized (execStatus) {
						if (id.equals(message)) {
							if (!execStatus.aborted) {
								// If not already aborted, mark as finished and stop abort timer.
								execStatus.finished = true;
								abortTimer.cancel();
							}
							// Always unsubscribe
							this.unsubscribe();
						}
					}
				}

			};

			// Set abort timer of 30 seconds
			abortTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					synchronized (execStatus) {
						if (!execStatus.finished) {
							// If execStatus is not finished, mark as aborted and unsubscribe from Redis
							execStatus.aborted = true;
							jedisSubscribe.unsubscribe();
						}
					}
				}

			}, 30000);

			// Start listening for response
			jedis.subscribe(jedisSubscribe, responseChannel);
			this.jedisPool.returnResource(jedis);

			// Just in case
			abortTimer.cancel();

			if (execStatus.aborted) {
				throw new GraphvizException(String.format("Timeout in vCat grid job %s", id));
			}

		}

	}

	@Override
	public void render(final File inputFile, final File outputFile, final GraphvizParams params)
			throws GraphvizException {
		try {
			this.exec(new File(programPath, params.getAlgorithm().getProgram()).getAbsolutePath(), "-T"
					+ params.getOutputFormat().getGraphvizTypeParameter(), "-o" + outputFile.getAbsolutePath(),
					inputFile.getAbsolutePath());
		} catch (GraphvizException e) {
			throw new GraphvizException(e);
		}
	}

}
