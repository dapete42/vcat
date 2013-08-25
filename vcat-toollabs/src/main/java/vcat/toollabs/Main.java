package vcat.toollabs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream.GetField;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

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

public class Main {

	private static final String CHANNEL_SUFFIX_REQUEST = "-requests";

	private static final String CHANNEL_SUFFIX_RESPONSE = "-responses";

	private static final String redisKeyRequestSuffix = "-request";

	private static final String redisKeyResponseErrorSuffix = "-response-error";

	private static final String redisKeyResponseHeadersSuffix = "-response-headers";

	private static final String redisKeyResponseSuffix = "-response";

	private static String cacheDir = "./cache";

	private static int purge = 600;

	private static int purgeMetadata = 86400;

	private static Log log = LogFactory.getLog(Main.class);

	private static String redisKeyPrefix;

	private static String redisSecret;

	private static String redisRequestChannel;

	private static String redisResponseChannel;

	public static void main(final String[] args) throws CacheException, GraphvizException, VCatException {

		if (args.length != 1) {
			log.error("This program expects the name of a .properties file as a command line parameter");
			return;
		}

		File propertiesFile = new File(args[0]);
		if (!propertiesFile.exists() || !propertiesFile.isFile() || !propertiesFile.canRead()) {
			log.error(".properties file '" + propertiesFile.getAbsolutePath() + "' must exist and be a readable file");
			return;
		}

		Properties properties = new Properties();
		try {
			BufferedReader propertiesReader = new BufferedReader(new InputStreamReader(new FileInputStream(
					propertiesFile), "UTF8"));
			properties.load(propertiesReader);
		} catch (IOException e) {
			log.error("Error reading .properties file '" + propertiesFile.getAbsolutePath() + "'", e);
			return;
		}

		redisSecret = "1234567890";

		redisKeyPrefix = redisSecret + "-";
		redisRequestChannel = redisSecret + CHANNEL_SUFFIX_REQUEST;
		redisResponseChannel = redisSecret + CHANNEL_SUFFIX_RESPONSE;

		log.info("Using redis secret " + redisSecret);
		log.info("All redis keys will use the prefix " + redisKeyPrefix);
		log.info("Redis request channel: " + redisRequestChannel);
		log.info("Redis response channel: " + redisRequestChannel);

		final String redisApiCacheKeyPrefix = redisKeyPrefix + "-api-cache-";
		final String redisMetadataCacheKeyPrefix = redisKeyPrefix + "-metadata-cache-";

		// Pool of Redis connections
		final JedisPool jedisPool = new JedisPool("localhost");
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

		JedisPubSub jedisSubscribe = new JedisPubSub() {

			@Override
			public void onMessage(final String channel, final String message) {
				renderJson(jedis, message, vCatRenderer);
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

		jedisListen.subscribe(jedisSubscribe, redisRequestChannel);

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
		final String errorMessage = e.getMessage();
		System.err.println(errorMessage);
		e.printStackTrace();
		jedis.set(redisKeyPrefix + jedisKey + redisKeyResponseErrorSuffix, errorMessage);
		jedis.expire(jedisKey, 60);
		jedis.publish(redisResponseChannel, jedisKey);
		jedis.del(redisKeyPrefix + jedisKey + redisKeyRequestSuffix);
	}

	protected static void renderJson(final Jedis jedis, final String jedisKey, final VCatRenderer vCatRenderer) {

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
					renderedFileInfo = vCatRenderer.render(parameterMap);
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
				jedis.publish(redisResponseChannel, jedisKey);

				// Clean up request
				jedis.del(jsonRequestKey);

				System.out.println("Headers submitted to redis");
				System.out.println("Results in " + renderedFileInfo.getFile().getAbsolutePath());
				System.out.println("Finished thread " + this.getName());
			}

			@Override
			public void start() {
				super.start();
				System.out.println("Started thread " + this.getName());
			}

		}.start();

	}

}
