package vcat.toollabs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import vcat.VCatException;
import vcat.VCatRenderer;
import vcat.VCatRenderer.RenderedFileInfo;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.cache.redis.ApiRedisCache;
import vcat.cache.redis.MetadataRedisCache;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.graphviz.GraphvizJNI;
import vcat.params.AllParams;

public class Main {

	private static final Log log = LogFactory.getLog(Main.class);

	private static String cacheDir;

	private static String jdbcUrl;

	private static String jdbcUser;

	private static String jdbcPassword;

	private static int purge;

	private static int purgeMetadata;

	private static String redisKeyPrefix;

	private static String redisSecret;

	private static String redisChannelControl;

	private static String redisChannelRequest;

	private static String redisChannelResponse;

	private static String redisKeyRequestSuffix;

	private static String redisKeyResponseErrorSuffix;

	private static String redisKeyResponseHeadersSuffix;

	private static String redisKeyResponseSuffix;

	private static String redisServerHostname;

	private static int redisServerPort;

	private static ToollabsMetainfoReader toollabsMetainfo;

	public static void main(final String[] args) throws CacheException, GraphvizException, VCatException {

		if (!initProperties(args)) {
			return;
		}

		final Connection connection;
		try {
			connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
		} catch (SQLException e) {
			throw new VCatException("Error connecting to database url '" + jdbcUrl + '\'', e);
		}
		toollabsMetainfo = new ToollabsMetainfoReader(connection);

		redisKeyPrefix = redisSecret + '-';

		final String redisApiCacheKeyPrefix = redisKeyPrefix + "cache-api-";
		final String redisMetadataCacheKeyPrefix = redisKeyPrefix + "cache-metadata-";

		// Pool of Redis connections
		final JedisPool jedisPool = new JedisPool(redisServerHostname, redisServerPort);
		// Get some for myself
		final Jedis jedis = jedisPool.getResource();
		final Jedis jedisListen = jedisPool.getResource();

		// Use Redis for API and metadata caches
		final IApiCache apiCache = new ApiRedisCache(jedisPool, redisApiCacheKeyPrefix, purge);
		final IMetadataCache metadataCache = new MetadataRedisCache(jedisPool, redisMetadataCacheKeyPrefix,
				purgeMetadata);
		// For other caches, use this directory
		final File tmpDir = new File(cacheDir);

		// Call Graphviz using JNI
		final Graphviz graphviz = new GraphvizJNI();

		// Create renderer
		final VCatRenderer vCatRenderer = new VCatRenderer(graphviz, tmpDir, apiCache, metadataCache, purge);

		final JedisPubSub jedisSubscribe = new JedisPubSub() {

			@Override
			public void onMessage(final String channel, final String message) {
				if (redisChannelControl.equals(channel)) {
					if ("stop".equalsIgnoreCase(message)) {
						log.info("Received STOP command on Redis control channel");
						this.unsubscribe();
					} else {
						log.warn("Received invalid command '" + message + "' on Redis control channel");
					}
				} else if (redisChannelRequest.equals(channel)) {
					log.info("Received Redis request '" + message + '\'');
					renderJson(jedis, message, vCatRenderer, metadataCache, apiCache);
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

		log.info("Start listening to Redis control channel '" + redisChannelControl + '\'');
		log.info("Start listening to Redis request channel '" + redisChannelRequest + '\'');
		jedisListen.subscribe(jedisSubscribe, redisChannelControl, redisChannelRequest);

	}

	private static boolean initProperties(final String[] args) {
		if (args.length != 1) {
			log.error("This program expects the name of a .properties file as a command line parameter");
			return false;
		}

		File propertiesFile = new File(args[0]);
		if (!propertiesFile.exists() || !propertiesFile.isFile() || !propertiesFile.canRead()) {
			log.error(".properties file '" + propertiesFile.getAbsolutePath() + "' must exist and be a readable file");
			return false;
		}

		Properties properties = new Properties();
		try {
			BufferedReader propertiesReader = new BufferedReader(new InputStreamReader(new FileInputStream(
					propertiesFile), "UTF8"));
			properties.load(propertiesReader);
		} catch (IOException e) {
			log.error("Error reading .properties file '" + propertiesFile.getAbsolutePath() + "'", e);
			return false;
		}

		int errors = 0;

		cacheDir = properties.getProperty("cache.dir");
		if (cacheDir == null || cacheDir.isEmpty()) {
			log.error("Property cache.dir missing or empty");
			errors++;
		}

		jdbcUrl = properties.getProperty("jdbc.url");
		if (jdbcUrl == null || jdbcUrl.isEmpty()) {
			log.error("Property jdbc.url missing or empty");
			errors++;
		}

		jdbcUser = properties.getProperty("jdbc.user");
		if (jdbcUser == null || jdbcUser.isEmpty()) {
			log.error("Property jdbc.user missing or empty");
			errors++;
		}

		jdbcPassword = properties.getProperty("jdbc.password");
		if (jdbcPassword == null || jdbcPassword.isEmpty()) {
			log.error("Property jdbc.password missing or empty");
			errors++;
		}

		final String purgeString = properties.getProperty("purge");
		if (purgeString == null || purgeString.isEmpty()) {
			purge = 60;
			log.info("Property purge not set, using default value " + purge);
		} else {
			try {
				purge = Integer.parseInt(purgeString);
			} catch (NumberFormatException e) {
				log.error("Property purge is not a number", e);
				errors++;
			}
		}

		final String purgeMetadataString = properties.getProperty("purge.metadata");
		if (purgeMetadataString == null || purgeMetadataString.isEmpty()) {
			purgeMetadata = 60;
			log.info("Property purge.metadata not set, using default value " + purgeMetadata);
		} else {
			try {
				purgeMetadata = Integer.parseInt(purgeMetadataString);
			} catch (NumberFormatException e) {
				log.error("Property purge.metadata is not a number", e);
				errors++;
			}
		}

		redisServerHostname = properties.getProperty("redis.server.hostname");
		if (redisServerHostname == null) {
			log.error("Property redis.server.hostname missing");
			errors++;
		}

		final String redisServerPortString = properties.getProperty("redis.server.port");
		if (redisServerPortString == null || redisServerPortString.isEmpty()) {
			redisServerPort = 6379;
			log.info("Property redis.server.port not set, using default value " + redisServerPort);
		} else {
			try {
				redisServerPort = Integer.parseInt(redisServerPortString);
			} catch (NumberFormatException e) {
				log.error("Property redis.server.port is not a number", e);
				errors++;
			}
		}

		redisSecret = properties.getProperty("redis.secret");
		if (redisSecret == null) {
			log.error("Property redis.secret missing");
			errors++;
		} else {
			log.info("Using redis secret " + redisSecret);
		}

		final String redisChannelControlSuffix = properties.getProperty("redis.channel.control.suffix");
		if (redisChannelControlSuffix == null || redisChannelControlSuffix.isEmpty()) {
			log.error("Property redis.channel.control.suffix missing or empty");
			errors++;
		} else {
			redisChannelControl = redisSecret + redisChannelControlSuffix;
		}

		final String redisChannelRequestSuffix = properties.getProperty("redis.channel.request.suffix");
		if (redisChannelRequestSuffix == null || redisChannelRequestSuffix.isEmpty()) {
			log.error("Property redis.channel.request.suffix missing or empty");
			errors++;
		} else {
			redisChannelRequest = redisSecret + redisChannelRequestSuffix;
		}

		final String redisChannelResponseSuffix = properties.getProperty("redis.channel.response.suffix");
		if (redisChannelResponseSuffix == null || redisChannelResponseSuffix.isEmpty()) {
			log.error("Property redis.channel.response.suffix missing or empty");
			errors++;
		} else {
			redisChannelResponse = redisSecret + redisChannelResponseSuffix;
		}

		redisKeyRequestSuffix = properties.getProperty("redis.key.request.suffix");
		if (redisKeyRequestSuffix == null || redisKeyRequestSuffix.isEmpty()) {
			log.error("Property redis.key.requests.suffix missing or empty");
			errors++;
		}

		redisKeyResponseErrorSuffix = properties.getProperty("redis.key.response.error.suffix");
		if (redisKeyResponseErrorSuffix == null || redisKeyResponseErrorSuffix.isEmpty()) {
			log.error("Property redis.key.response.error.suffix missing or empty");
			errors++;
		}

		redisKeyResponseHeadersSuffix = properties.getProperty("redis.key.response.headers.suffix");
		if (redisKeyResponseHeadersSuffix == null || redisKeyResponseHeadersSuffix.isEmpty()) {
			log.error("Property redis.key.response.headers.suffix missing or empty");
			errors++;
		}

		redisKeyResponseSuffix = properties.getProperty("redis.key.response.suffix");
		if (redisKeyResponseSuffix == null || redisKeyResponseSuffix.isEmpty()) {
			log.error("Property redis.key.response.suffix missing or empty");
			errors++;
		}

		return errors == 0;
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
		jedis.set(redisKeyPrefix + jedisKey + redisKeyResponseErrorSuffix, e.getMessage());
		jedis.expire(jedisKey, 60);
		jedis.publish(redisChannelResponse, jedisKey);
		jedis.del(redisKeyPrefix + jedisKey + redisKeyRequestSuffix);
	}

	protected static void renderJson(final Jedis jedis, final String jedisKey, final VCatRenderer vCatRenderer,
			final IMetadataCache metadataCache, final IApiCache apiCache) {

		final String jsonRequestKey = redisKeyPrefix + jedisKey + redisKeyRequestSuffix;
		final String jsonResponseHeadersKey = redisKeyPrefix + jedisKey + redisKeyResponseHeadersSuffix;
		final String responseKey = redisKeyPrefix + jedisKey + redisKeyResponseSuffix;

		final HashMap<String, String[]> parameterMap = new HashMap<String, String[]>();
		try {
			fillParametersFromJson(parameterMap, jedis.get(jsonRequestKey));
		} catch (VCatException e) {
			handleError(jedis, jedisKey, e);
			return;
		}

		new Thread() {

			@Override
			public void run() {
				super.run();
				RenderedFileInfo renderedFileInfo;
				try {
					final AllParams all = new AllParamsToollabs(parameterMap, apiCache, metadataCache, toollabsMetainfo);
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
				long receivers = jedis.publish(redisChannelResponse, jedisKey);

				// If nobody received the message, something was wrong
				if (receivers == 0) {
					log.error("Response for job '" + jedisKey
							+ "' was sent to Redis response channel, but nobody was listening");
				} else {
					log.info("Response for job '" + jedisKey + "' was sent to Redis response channel");
				}

				// Clean up request
				jedis.del(jsonRequestKey);

				log.info("Finished thread '" + this.getName() + "'for job '" + jedisKey + '\'');
			}

			@Override
			public void start() {
				super.start();
				log.info("Started thread '" + this.getName() + "'for job '" + jedisKey + '\'');
			}

		}.start();

	}

}
