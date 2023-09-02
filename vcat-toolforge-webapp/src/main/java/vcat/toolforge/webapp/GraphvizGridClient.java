package vcat.toolforge.webapp;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.helpers.MessageFormatter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.params.GraphvizParams;
import vcat.redis.SimplePubSub;

import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

public class GraphvizGridClient implements Graphviz {

    private static class ExecStatus {

        boolean aborted = false;

        boolean finished = false;

    }

    private final JedisPool jedisPool;

    private final String redisSecret;

    private final Path programPath;

    public GraphvizGridClient(final JedisPool jedisPool, final String redisSecret, final Path programPath) {
        this.jedisPool = jedisPool;
        this.redisSecret = redisSecret;
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

        try (Jedis jedis = this.jedisPool.getResource()) {

            // Put message in Redis
            jedis.set(requestPrefix + id, message);
            // Publish id to Redis request channel
            final long listening = jedis.publish(requestChannel, id);

            if (listening == 0) {
                throw new GraphvizException(
                        MessageFormatter.format("There appear to be no gridservers running (id {})", id).getMessage());
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

            // Just in case
            abortTimer.cancel();

            if (execStatus.aborted) {
                throw new GraphvizException(MessageFormatter.format("Timeout in vCat grid job {}", id).getMessage());
            }

        }

    }

    @Override
    public void render(final Path inputFile, final Path outputFile, final GraphvizParams params)
            throws GraphvizException {
        try {
            this.exec(programPath.resolve(params.getAlgorithm().getProgram()).toAbsolutePath().toString(),
                    "-T" + params.getOutputFormat().getGraphvizTypeParameter(),
                    "-o" + outputFile.toAbsolutePath().toString(), inputFile.toAbsolutePath().toString());
        } catch (GraphvizException e) {
            throw new GraphvizException(e);
        }
    }

}
