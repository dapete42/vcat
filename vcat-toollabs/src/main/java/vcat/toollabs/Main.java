package vcat.toollabs;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.FileUtils;
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

	private static final String COMMAND_STATS = "stats";

	private static final String COMMAND_STOP = "stop";

	private static final MainConfig config = new MainConfig();

	private static final MyCnfConfig configMyCnf = new MyCnfConfig();

	private static ExecutorService executorService;

	private static JedisPool jedisPool;

	private static final long KB = 1024;

	private static final long MB = KB * KB;

	private static boolean running = true;

	private static File tempDir;

	private static ToollabsWikiProvider toollabsMetainfo;

	public static void main(final String[] args) throws CacheException, GraphvizException, VCatException {

		if (!initConfig(args)) {
			return;
		}

		// Pool for database connections
		final ComboPooledDataSource cpds = new ComboPooledDataSource();
		cpds.setJdbcUrl(config.jdbcUrl);
		cpds.setUser(configMyCnf.user);
		cpds.setPassword(configMyCnf.password);
		// Stay small and close connections quickly - this is only used for metadata for now, so it's not used much
		cpds.setInitialPoolSize(1);
		cpds.setMinPoolSize(0);
		cpds.setMaxPoolSize(10);
		cpds.setAcquireIncrement(1);
		cpds.setMaxIdleTime(600);
		cpds.setMaxConnectionAge(3600);

		toollabsMetainfo = new ToollabsWikiProvider(cpds);

		final String redisApiCacheKeyPrefix = config.redisSecret + '-' + "cache-api-";
		final String redisMetadataCacheKeyPrefix = config.redisSecret + '-' + "cache-metadata-";

		// Conservative configuration for Redis connection pool - check connections as often as possible
		final JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		// Allow some more concurrent connections
		poolConfig.setMaxTotal(16);
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

		// For cache of Graphviz files and rendered images, use this directory
		final File cacheDir = new File(config.cacheDir);
		// Temporary directory for Graphviz files and rendered images
		tempDir = new File(config.tempDir);

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

			private Date lastRequestDate = null;

			private long requests = 0;

			@Override
			public void onMessage(final String channel, final String message) {
				if (config.redisChannelControl.equals(channel)) {
					if (COMMAND_PING.equalsIgnoreCase(message)) {
						// Do nothing
					} else if (COMMAND_STATS.equalsIgnoreCase(message)) {
						log.info(Messages.getString("Main.Info.ControlStats"));
						final String lastRequestString;
						if (lastRequestDate == null) {
							lastRequestString = Messages.getString("Main.Info.StatsNever");
						} else {
							lastRequestString = SimpleDateFormat.getDateTimeInstance().format(this.lastRequestDate);
						}
						final Runtime r = Runtime.getRuntime();
						final long maxMemory = r.maxMemory() / MB;
						final long totalMemory = r.totalMemory() / MB;
						final long freeMemory = r.freeMemory() / MB;
						final int jdbcPoolMaxConnections = cpds.getMaxPoolSize();
						int jdbcPoolOpenConnections = -1;
						int jdbcPoolBusyConnections = -1;
						try {
							jdbcPoolOpenConnections = cpds.getNumConnections();
							jdbcPoolBusyConnections = cpds.getNumBusyConnections();
						} catch (SQLException e) {
							log.error(e);
						}
						final long fileCacheSize = FileUtils.sizeOfDirectory(cacheDir);
						log.info(String.format(Messages.getString("Main.Info.Stats"), this.requests, lastRequestString,
								maxMemory, totalMemory, freeMemory, jdbcPoolMaxConnections, jdbcPoolOpenConnections,
								jdbcPoolBusyConnections, fileCacheSize));
					} else if (COMMAND_STOP.equalsIgnoreCase(message)) {
						log.info(Messages.getString("Main.Info.ControlStop"));
						// Set flag so listening connection is not re-established
						running = false;
						// Unsubscribing causes execution of the main program to continue
						this.unsubscribe();
					} else {
						log.warn(String.format(Messages.getString("Main.Warn.ControlInvalidCommand"), message));
					}
				} else if (config.redisChannelRequest.equals(channel)) {
					this.lastRequestDate = new Date();
					this.requests++;
					log.info(String.format(Messages.getString("Main.Info.RequestReceived"), message));
					renderJson(message, vCatRenderer, metadataProvider);
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

	private static void fillParametersFromJson(final HashMap<String, String[]> parameterMap,
			final JSONObject jsonParameters) throws VCatException {
		try {
			JSONArray jsonArrayNames = jsonParameters.names();
			for (int i = 0; i < jsonArrayNames.length(); i++) {
				final String jsonArrayName = jsonArrayNames.getString(i);
				final JSONArray jsonArray = jsonParameters.getJSONArray(jsonArrayName);
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

	private static void handleError(final Jedis jedis, final String requestKey, final Exception e) {
		// There has been an error, but it is handled gracefully; so a warning is enough
		log.warn(e);
		e.printStackTrace();
		final JSONObject json = new JSONObject();
		json.put("key", requestKey);
		json.put("status", "error");
		json.put("error", e.getMessage());
		final String jsonString = json.toString();
		jedis.publish(config.redisChannelResponse, json.toString());
		log.info(String.format(Messages.getString("Main.Info.ResponseSent"), jsonString));
	}

	private static void renderJson(final String jsonString, final VCatRenderer<ToollabsWiki> vCatRenderer,
			final IMetadataProvider metadataProvider) {

		// Get Jedis connection from pool
		final Jedis jedis = jedisPool.getResource();

		try {

			final JSONObject jsonRequest = new JSONObject(new JSONTokener(jsonString));
			final String requestKey = jsonRequest.getString("key");

			try {

				final HashMap<String, String[]> parameterMap = new HashMap<String, String[]>();
				try {
					fillParametersFromJson(parameterMap, jsonRequest.getJSONObject("parameters"));
				} catch (VCatException e) {
					handleError(jedis, requestKey, e);
					return;
				}

				// Return Jedis connection to pool
				jedisPool.returnResource(jedis);

				executorService.execute(new Runnable() {

					@Override
					public void run() {
						log.info(String.format(Messages.getString("Main.Info.ThreadStart"), Thread.currentThread()
								.getName(), requestKey));

						// Get Jedis connection from pool
						final Jedis jedis = jedisPool.getResource();

						try {

							VCatRenderer<ToollabsWiki>.RenderedFileInfo renderedFileInfo;
							try {
								final AllParamsToollabs all = new AllParamsToollabs(parameterMap, config.renderUrl,
										metadataProvider, toollabsMetainfo);
								renderedFileInfo = vCatRenderer.render(all, tempDir);
							} catch (VCatException e) {
								handleError(jedis, requestKey, e);
								return;
							}

							JSONObject jsonResponseHeaders = new JSONObject();
							try {
								// Content-type, as returned from rendering process
								final String contentType = renderedFileInfo.getMimeType();
								jsonResponseHeaders.put("Content-type", contentType);
								// Content-length, using length of temporary output file already written
								final long length = renderedFileInfo.getFile().length();
								if (length < Integer.MAX_VALUE) {
									jsonResponseHeaders.put("Content-length", Long.toString(length));
								}
								// Content-disposition, to determine filename of returned contents
								final String filename = renderedFileInfo.getFile().getName();
								jsonResponseHeaders.put("Content-disposition", "filename=\"" + filename + "\"");
								// Control caching behaviour
								// Although we have our own cache, let proxys and browsers also cache for a while. Half
								// of
								// our own purging interval seems fitting (so updates are still possible).
								final String cacheControl = "max-age=" + (config.purge / 2);
								jsonResponseHeaders.put("Cache-Control", cacheControl);
							} catch (JSONException e) {
								handleError(jedis, requestKey, e);
								return;
							}

							JSONObject jsonResponse = new JSONObject();
							jsonResponse.put("key", requestKey);
							jsonResponse.put("headers", jsonResponseHeaders);
							jsonResponse.put("filename", renderedFileInfo.getFile().getAbsolutePath());

							// Send response
							String jsonResponseString = jsonResponse.toString();
							long receivers = jedis.publish(config.redisChannelResponse, jsonResponseString);

							log.info(String.format(Messages.getString("Main.Info.ResponseSent"), jsonResponseString));

							// If nobody received the message, something was wrong
							if (receivers == 0) {
								log.error(String.format(Messages.getString("Main.Error.ResponseNobodyListening"),
										requestKey));
							}

						} catch (Exception e) {
							// All exceptions are caught so client is informed of error, if possible
							handleError(jedis, requestKey, e);
						} finally {
							// Return Jedis connection to pool
							jedisPool.returnResource(jedis);
						}

						log.info(String.format(Messages.getString("Main.Info.ThreadFinish"), Thread.currentThread()
								.getName(), requestKey));

					}

				});

			} catch (Exception e) {
				// All exceptions are caught to prevent the daemon from crashing
				handleError(jedis, requestKey, e);
			}

		} catch (Exception e) {
			// All exceptions are caught to prevent the daemon from crashing
		}

		finally {
			jedisPool.returnResource(jedis);
		}

	}
}
