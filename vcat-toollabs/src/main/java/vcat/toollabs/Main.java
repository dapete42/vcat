package vcat.toollabs;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import vcat.VCatException;
import vcat.VCatRenderer;
import vcat.VCatRenderer.RenderedFileInfo;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.cache.redis.ApiRedisCache;
import vcat.cache.redis.MetadataRedisCache;
import vcat.graphviz.GraphvizException;
import vcat.graphviz.GraphvizExternal;
import vcat.params.AllParams;

public class Main {

	private static final Log log = LogFactory.getLog(Main.class);

	private static final MainConfig config = new MainConfig();

	private static JedisPool jedisPool;

	private static boolean running = true;

	private static ToollabsMetainfoReader toollabsMetainfo;

	public static void main(final String[] args) throws CacheException, GraphvizException, VCatException {

		if (!initConfig(args)) {
			return;
		}

		final Connection connection;
		try {
			connection = DriverManager.getConnection(config.jdbcUrl, config.jdbcUser, config.jdbcPassword);
		} catch (SQLException e) {
			throw new VCatException("Error connecting to database url '" + config.jdbcUrl + '\'', e);
		}
		toollabsMetainfo = new ToollabsMetainfoReader(connection);

		final String redisApiCacheKeyPrefix = config.redisSecret + '-' + "cache-api-";
		final String redisMetadataCacheKeyPrefix = config.redisSecret + '-' + "cache-metadata-";

		// Pool of Redis connections
		jedisPool = new JedisPool(config.redisServerHostname, config.redisServerPort);

		// Use Redis for API and metadata caches
		final IApiCache apiCache = new ApiRedisCache(jedisPool, redisApiCacheKeyPrefix, config.purge);
		final IMetadataCache metadataCache = new MetadataRedisCache(jedisPool, redisMetadataCacheKeyPrefix,
				config.purgeMetadata);

		// For other caches, use this directory
		final File cacheDir = new File(config.cacheDir);

		// Call Graphviz using JNI
		// final GraphvizJsub graphviz = new GraphvizJsub(new File("/usr/bin"), 512);
		// Call Graphviz executables directly
		final GraphvizExternal graphviz = new GraphvizExternal(new File("/usr/bin"));

		// Create renderer
		final VCatRenderer vCatRenderer = new VCatRenderer(graphviz, cacheDir, apiCache, metadataCache, config.purge);

		final JedisPubSub jedisSubscribe = new JedisPubSub() {

			@Override
			public void onMessage(final String channel, final String message) {
				if (config.redisChannelControl.equals(channel)) {
					if ("stop".equalsIgnoreCase(message)) {
						log.info("Received STOP command on Redis control channel");
						// Set flag so listening connectin is not re-established
						running = false;
						// Unsubscribing causes the main program to continue running
						this.unsubscribe();
					} else {
						log.warn("Received invalid command '" + message + "' on Redis control channel");
					}
				} else if (config.redisChannelRequest.equals(channel)) {
					log.info("Received Redis request '" + message + '\'');
					renderJson(message, vCatRenderer, metadataCache, apiCache);
				}
			}

			@Override
			public void onSubscribe(final String channel, final int subscribedChannels) {
			}

			@Override
			public void onPUnsubscribe(final String pattern, final int subscribedChannels) {
			}

			@Override
			public void onPMessage(final String pattern, final String channel, final String message) {
			}

			@Override
			public void onPSubscribe(final String pattern, final int subscribedChannels) {
			}

			@Override
			public void onUnsubscribe(final String channel, final int subscribedChannels) {
			}

		};

		log.info("Start listening to Redis control channel '" + config.redisChannelControl + '\'');
		log.info("Start listening to Redis request channel '" + config.redisChannelRequest + '\'');

		while (running) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				jedis.subscribe(jedisSubscribe, config.redisChannelControl, config.redisChannelRequest);
			} catch (JedisException je) {
				// Most likely the connection has been lost. Resource is broken.
				jedisPool.returnBrokenResource(jedis);
				log.warn("Jedis error, re-establishing connection", je);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					// Ignore, does no harm
				}
			}
		}

	}

	private static boolean initConfig(final String[] args) throws VCatException {
		if (args.length != 1) {
			log.error("This program expects the name of a .properties file as a command line parameter");
			return false;
		}

		File propertiesFile = new File(args[0]);
		if (!propertiesFile.exists() || !propertiesFile.isFile() || !propertiesFile.canRead()) {
			log.error(".properties file '" + propertiesFile.getAbsolutePath() + "' must exist and be a readable file");
			return false;
		}

		return config.readFromPropertyFile(propertiesFile);
	}

	private static void fillParametersFromJson(final HashMap<String, String[]> parameterMap, final String jsonString)
			throws VCatException {
		try {
			final JSONObject json = new JSONObject(new JSONTokener(jsonString));
			JSONArray jsonArrayNames = json.names();
			for (int i = 0; i < jsonArrayNames.length(); i++) {
				final String jsonArrayName = jsonArrayNames.getString(i);
				final JSONArray jsonArray = json.getJSONArray(jsonArrayName);
				final String[] parameterArray = new String[jsonArray.length()];
				for (int j = 0; j < jsonArray.length(); j++) {
					parameterArray[j] = jsonArray.getString(j);
				}
				parameterMap.put(jsonArrayName, parameterArray);
			}
		} catch (JSONException e) {
			throw new VCatException("Error parsing json", e);
		}
	}

	private static void handleError(final Jedis jedis, final String jedisKey, final Exception e) {
		log.error(e);
		e.printStackTrace();
		jedis.set(config.buildRedisKeyResponseError(jedisKey), e.getMessage());
		jedis.expire(jedisKey, 60);
		jedis.publish(config.redisChannelResponse, jedisKey);
		jedis.del(config.buildRedisKeyRequest(jedisKey));
	}

	protected static void renderJson(final String jedisKey, final VCatRenderer vCatRenderer,
			final IMetadataCache metadataCache, final IApiCache apiCache) {

		try {

			final String jsonRequestKey = config.buildRedisKeyRequest(jedisKey);
			final String jsonResponseHeadersKey = config.buildRedisKeyResponseHeaders(jedisKey);
			final String responseKey = config.buildRedisKeyResponse(jedisKey);

			// Get Jedis connection from pool
			final Jedis jedis = jedisPool.getResource();

			final String jsonString = jedis.get(jsonRequestKey);
			if (jsonString == null) {
				throw new VCatException("Redis request data not found: " + jsonRequestKey);
			}

			final HashMap<String, String[]> parameterMap = new HashMap<String, String[]>();
			try {
				fillParametersFromJson(parameterMap, jsonString);
			} catch (VCatException e) {
				handleError(jedis, jedisKey, e);
				return;
			}

			// Return Jedis connection to pool
			jedisPool.returnResource(jedis);

			new Thread() {

				@Override
				public void run() {
					super.run();

					// Get Jedis connection from pool
					final Jedis jedis = jedisPool.getResource();

					RenderedFileInfo renderedFileInfo;
					try {
						final AllParams all = new AllParamsToollabs(parameterMap, apiCache, metadataCache,
								toollabsMetainfo);
						renderedFileInfo = vCatRenderer.render(all);
					} catch (VCatException e) {
						handleError(jedis, jedisKey, e);
						jedis.del(jsonRequestKey);
						return;
					}

					JSONObject json = new JSONObject();
					try {
						// Content-type, as returned from rendering process
						String contentType = renderedFileInfo.getMimeType();
						json.put("Content-type", contentType);
						// Content-length, using length of temporary output file already written
						long length = renderedFileInfo.getFile().length();
						if (length < Integer.MAX_VALUE) {
							json.put("Content-length", Long.toString(length));
						}
						// Content-disposition, to determine filename of returned contents
						String filename = renderedFileInfo.getFile().getName();
						json.put("Content-disposition", "filename=\"" + filename + "\"");
					} catch (JSONException e) {
						handleError(jedis, jedisKey, e);
						return;
					}

					// Set return values
					jedis.set(jsonResponseHeadersKey, json.toString());
					jedis.set(responseKey, renderedFileInfo.getFile().getAbsolutePath());
					// Values last for 60 seconds
					jedis.expire(jsonResponseHeadersKey, 60);
					jedis.expire(responseKey, 60);
					// Notify client that response is ready
					long receivers = jedis.publish(config.redisChannelResponse, jedisKey);

					// If nobody received the message, something was wrong
					if (receivers == 0) {
						log.error("Response for job '" + jedisKey
								+ "' was sent to Redis response channel, but nobody was listening");
					} else {
						log.info("Response for job '" + jedisKey + "' was sent to Redis response channel");
					}

					// Clean up request
					jedis.del(jsonRequestKey);

					// Return Jedis connection to pool
					jedisPool.returnResource(jedis);

					log.info("Finished thread '" + this.getName() + "' for job '" + jedisKey + '\'');

				}

				@Override
				public void start() {
					super.start();
					log.info("Started thread '" + this.getName() + "' for job '" + jedisKey + '\'');
				}

			}.start();

		} catch (Exception e) {
			// All exceptions are caught to prevent the daemon from crashing
			Jedis jedis = jedisPool.getResource();
			handleError(jedis, jedisKey, e);
			jedisPool.returnResource(jedis);
		}

	}
}
