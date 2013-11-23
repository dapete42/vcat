package vcat.toollabs;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import vcat.VCatException;
import vcat.VCatRenderer;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.graphviz.GraphvizException;
import vcat.graphviz.GraphvizExternal;
import vcat.graphviz.QueuedGraphviz;
import vcat.mediawiki.CachedApiClient;
import vcat.mediawiki.CachedMetadataProvider;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IMetadataProvider;
import vcat.redis.SimplePubSub;
import vcat.redis.cache.ApiRedisCache;
import vcat.redis.cache.MetadataRedisCache;
import vcat.toollabs.params.AllParamsToollabs;
import vcat.toollabs.util.ThreadHelper;

public class Main {

	private static final Log log = LogFactory.getLog(Main.class);

	private static final String COMMAND_PING = "ping";

	private static final String COMMAND_STOP = "stop";

	private static final MainConfig config = new MainConfig();

	private static final MyCnfConfig configMyCnf = new MyCnfConfig();

	private static ExecutorService executorService;

	private static JedisPool jedisPool;

	private static boolean running = true;

	private static ToollabsWikiProvider toollabsMetainfo;

	public static void main(final String[] args) throws CacheException, GraphvizException, VCatException {

		if (!initConfig(args)) {
			return;
		}

		ComboPooledDataSource cpds = new ComboPooledDataSource();
		cpds.setJdbcUrl(config.jdbcUrl);
		cpds.setUser(configMyCnf.user);
		cpds.setPassword(configMyCnf.password);
		cpds.setInitialPoolSize(1);
		cpds.setMinPoolSize(1);
		cpds.setMaxPoolSize(10);
		cpds.setAcquireIncrement(1);
		toollabsMetainfo = new ToollabsWikiProvider(cpds);

		final String redisApiCacheKeyPrefix = config.redisSecret + '-' + "cache-api-";
		final String redisMetadataCacheKeyPrefix = config.redisSecret + '-' + "cache-metadata-";

		// Conservative configuration for Redis connection pool - check connections as often as possible
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		// Allow some more concurrent connections
		poolConfig.setMaxActive(16);
		// We expect low traffic most of the time, so don't keep many idle connections open
		poolConfig.setMaxIdle(1);
		// Keep one spare idle connection
		poolConfig.setMinIdle(1);

		// Pool of Redis connections
		jedisPool = new JedisPool(poolConfig, config.redisServerHostname, config.redisServerPort);

		// Use Redis for API and metadata caches
		final IApiCache apiCache = new ApiRedisCache(jedisPool, redisApiCacheKeyPrefix, config.purge);
		final IMetadataCache metadataCache = new MetadataRedisCache(jedisPool, redisMetadataCacheKeyPrefix,
				config.purgeMetadata);

		// For other caches, use this directory
		final File cacheDir = new File(config.cacheDir);

		// Call external executables, but use a QueuedGraphviz to limit number of concurrent processes.
		final QueuedGraphviz graphviz = new QueuedGraphviz(new GraphvizExternal(new File(config.graphvizDir)),
				config.graphvizProcesses);

		final CachedApiClient<ToollabsWiki> apiClient = new CachedApiClient<ToollabsWiki>(apiCache);

		final IMetadataProvider metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);

		final ICategoryProvider<ToollabsWiki> categoryProvider = apiClient;
		// final ToollabsConnectionBuilder connectionBuilder = new ToollabsConnectionBuilder(config.jdbcUser,
		// config.jdbcPassword);
		// final ICategoryProvider<ToollabsWiki> categoryProvider = new ToollabsCategoryProvider(connectionBuilder,
		// metadataProvider);

		// Create renderer
		final VCatRenderer<ToollabsWiki> vCatRenderer = new VCatRenderer<ToollabsWiki>(graphviz, cacheDir,
				categoryProvider, config.purge);

		// Executor service for threads
		final ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat(Main.class.getSimpleName() + "-vcat-pool-%d");
		final ThreadFactory tf = tfb.build();
		executorService = Executors.newCachedThreadPool(tf);

		final SimplePubSub jedisSubscribe = new SimplePubSub() {

			@Override
			public void onMessage(final String channel, final String message) {
				if (config.redisChannelControl.equals(channel)) {
					switch (message) {
					case COMMAND_PING:
						// Do nothing,
						break;
					case COMMAND_STOP:
						log.info(Messages.getString("Main.Info.ControlStop"));
						// Set flag so listening connection is not re-established
						running = false;
						// Unsubscribing causes execution of the main program to continue
						this.unsubscribe();
						break;
					default:
						log.warn(String.format(Messages.getString("Main.Warn.CrontolInvalidCommand"), message));
					}
				} else if (config.redisChannelRequest.equals(channel)) {
					log.info(String.format(Messages.getString("Main.Info.RequestReceived"), message));
					renderJson(message, vCatRenderer, metadataProvider, apiCache);
				}
			}

		};

		log.info(String.format(Messages.getString("Main.Info.ControlListen"), config.redisChannelControl));
		log.info(String.format(Messages.getString("Main.Info.RequestListen"), config.redisChannelRequest));

		while (running) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				jedis.subscribe(jedisSubscribe, config.redisChannelControl, config.redisChannelRequest);
			} catch (JedisException je) {
				// Most likely the connection has been lost. Resource is broken.
				jedisPool.returnBrokenResource(jedis);
				log.warn(Messages.getString("Main.Warn.JedisConnection"), je);
				ThreadHelper.sleep(1000);
			}
		}

		cpds.close();

	}

	private static boolean initConfig(final String[] args) throws VCatException {
		if (args.length != 1) {
			log.error(Messages.getString("Main.Error.ParameterMissing"));
			return false;
		}

		File propertiesFile = new File(args[0]);
		if (!propertiesFile.exists() || !propertiesFile.isFile() || !propertiesFile.canRead()) {
			log.error(String.format(Messages.getString("Main.Error.PropertiesNotFound"),
					propertiesFile.getAbsolutePath()));
			return false;
		}

		return config.readFromPropertyFile(propertiesFile) && configMyCnf.readFromMyCnf();
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
			throw new VCatException(Messages.getString("Main.Exception.Json"), e);
		}
	}

	private static void handleError(final Jedis jedis, final String jedisKey, final Exception e) {
		// There has been an error, but is is handled gracefully; so a warning is enough
		log.warn(e);
		e.printStackTrace();
		jedis.del(config.buildRedisKeyRequest(jedisKey));
		Transaction t = jedis.multi();
		t.set(config.buildRedisKeyResponseError(jedisKey), e.getMessage());
		t.expire(jedisKey, 60);
		t.publish(config.redisChannelResponse, jedisKey);
		t.exec();
	}

	private static void renderJson(final String jedisKey, final VCatRenderer<ToollabsWiki> vCatRenderer,
			final IMetadataProvider metadataProvider, final IApiCache apiCache) {

		try {

			final String jsonRequestKey = config.buildRedisKeyRequest(jedisKey);
			final String jsonResponseHeadersKey = config.buildRedisKeyResponseHeaders(jedisKey);
			final String responseKey = config.buildRedisKeyResponse(jedisKey);

			// Get Jedis connection from pool
			final Jedis jedis = jedisPool.getResource();

			final String jsonString = jedis.get(jsonRequestKey);
			if (jsonString == null) {
				throw new VCatException(String.format(Messages.getString("Main.Exception.RequestDataNotFound"),
						jsonRequestKey));
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

			executorService.execute(new Runnable() {

				@Override
				public void run() {
					log.info(String.format(Messages.getString("Main.Info.ThreadStart"), Thread.currentThread()
							.getName(), jedisKey));

					// Get Jedis connection from pool
					final Jedis jedis = jedisPool.getResource();

					try {

						VCatRenderer<ToollabsWiki>.RenderedFileInfo renderedFileInfo;
						try {
							final AllParamsToollabs all = new AllParamsToollabs(parameterMap, metadataProvider,
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
							final String contentType = renderedFileInfo.getMimeType();
							json.put("Content-type", contentType);
							// Content-length, using length of temporary output file already written
							final long length = renderedFileInfo.getFile().length();
							if (length < Integer.MAX_VALUE) {
								json.put("Content-length", Long.toString(length));
							}
							// Content-disposition, to determine filename of returned contents
							final String filename = renderedFileInfo.getFile().getName();
							json.put("Content-disposition", "filename=\"" + filename + "\"");
							// Control caching behaviour
							// Although we have our own cache, let proxys and browsers also cache for a while. Half of
							// our own purging interval seems fitting (so updates are still possible).
							final String cacheControl = "max-age=" + (config.purge / 2);
							json.put("Cache-Control", cacheControl);
						} catch (JSONException e) {
							handleError(jedis, jedisKey, e);
							return;
						}

						Transaction t = jedis.multi();
						// Set return values
						t.set(jsonResponseHeadersKey, json.toString());
						t.set(responseKey, renderedFileInfo.getFile().getAbsolutePath());
						// Values last for 60 seconds
						t.expire(jsonResponseHeadersKey, 60);
						t.expire(responseKey, 60);

						t.exec();

						// Notify client that response is ready
						long receivers = jedis.publish(config.redisChannelResponse, jedisKey);

						// If nobody received the message, something was wrong
						if (receivers == 0) {
							log.error(String.format(Messages.getString("Main.Error.ResponseNobodyListening"), jedisKey));
						} else {
							log.info(String.format(Messages.getString("Main.Info.ResponseSent"), jedisKey));
						}

						// Clean up request
						jedis.del(jsonRequestKey);

					} catch (Exception e) {
						// All exceptions are caught so client is informed of error, if possible
						handleError(jedis, jedisKey, e);
					} finally {
						// Return Jedis connection to pool
						jedisPool.returnResource(jedis);
					}

					log.info(String.format(Messages.getString("Main.Info.ThreadFinish"), Thread.currentThread()
							.getName(), jedisKey));

				}

			});

		} catch (Exception e) {
			// All exceptions are caught to prevent the daemon from crashing
			Jedis jedis = jedisPool.getResource();
			handleError(jedis, jedisKey, e);
			jedisPool.returnResource(jedis);
		}

	}
}
