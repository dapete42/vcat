package vcat.toolforge.webapp;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import vcat.VCatException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.graphviz.Graphviz;
import vcat.graphviz.QueuedGraphviz;
import vcat.mediawiki.CachedApiClient;
import vcat.mediawiki.CachedMetadataProvider;
import vcat.mediawiki.IMetadataProvider;
import vcat.redis.cache.ApiRedisCache;
import vcat.redis.cache.MetadataRedisCache;
import vcat.renderer.CachedVCatRenderer;
import vcat.renderer.QueuedVCatRenderer;
import vcat.renderer.RenderedFileInfo;
import vcat.toolforge.base.AllParamsToolforge;
import vcat.toolforge.base.MyCnfConfig;
import vcat.toolforge.base.ToolforgeWiki;
import vcat.toolforge.base.ToolforgeWikiProvider;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@ApplicationScoped
@WebServlet(urlPatterns = {"/render", ToolforgeVCatServlet.CATGRAPH_REDIRECT_URL_PATTERN})
public class ToolforgeVCatServlet extends AbstractVCatToolforgeServlet {

    @Serial
    private static final long serialVersionUID = -5655389767357096359L;

    protected static final String CATGRAPH_REDIRECT_URL_PATTERN = "/catgraphRedirect";

    /**
     * Directory with Graphviz binaries (dot, fdp).
     */
    @Inject
    @ConfigProperty(name = "graphviz.dir", defaultValue = "/usr/bin")
    String graphvizDir;

    @Inject
    @ConfigProperty(name = "home.cache.dir", defaultValue = "work/cache")
    String homeCacheDir;

    @Inject
    @ConfigProperty(name = "home.temp.dir", defaultValue = "work/temp")
    String homeTempDir;

    /**
     * JDBC URL for MySQL/MariaDB access to wiki table.
     */
    @Inject
    @ConfigProperty(name = "jdbc.url")
    String jdbcUrl;

    /**
     * Purge caches after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge", defaultValue = "600")
    Integer cachePurge;

    /**
     * Purge metadata after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge.metadata", defaultValue = "86400")
    Integer cachePurgeMetadata;

    /**
     * Redis server hostname.
     */
    @Inject
    @ConfigProperty(name = "redis.hostname")
    String redisHostname = "tools-redis";

    /**
     * Redis server port.
     */
    @Inject
    @ConfigProperty(name = "redis.port")
    Integer redisPort;

    /**
     * Maxim number of concurrent threads running graphviz (0=unlimited).
     */
    @Inject
    @ConfigProperty(name = "graphviz.threads", defaultValue = "0")
    Integer graphvizThreads;

    /**
     * Maximum number of concurrent threads running vCat (0=unlimited).
     */
    @Inject
    @ConfigProperty(name = "vcat.threads", defaultValue = "0")
    Integer vcatThreads;

    /**
     * Maximum number of processes to queue vor vCat
     */
    @Inject
    @ConfigProperty(name = "vcat.queue", defaultValue = "100")
    Integer vcatQueue;

    private static QueuedVCatRenderer<ToolforgeWiki> vCatRenderer;

    private static IMetadataProvider metadataProvider;

    private static ToolforgeWikiProvider toolforgeWikiProvider;

    @Override
    public String getServletInfo() {
        return Messages.getString("ToolforgeVCatServlet.ServletInfo");
    }

    @Override
    public void init() throws ServletException {

        try {

            MyCnfConfig configMyCnf = new MyCnfConfig();
            configMyCnf.readFromMyCnf();

            // Pool for database connections
            final ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setJdbcUrl(jdbcUrl);
            try {
                // Fails for some reason unless explicitly set
                cpds.setDriverClass(org.mariadb.jdbc.Driver.class.getName());
            } catch (PropertyVetoException e) {
                // ignore
            }
            cpds.setUser(configMyCnf.getUser());
            cpds.setPassword(configMyCnf.getPassword());
            // Stay small and close connections quickly - this is only used for metadata for now, so it's not used much
            cpds.setInitialPoolSize(1);
            cpds.setMinPoolSize(0);
            cpds.setMaxPoolSize(10);
            cpds.setAcquireIncrement(1);
            cpds.setMaxIdleTime(600);
            cpds.setMaxConnectionAge(3600);

            // Provider for Wikimedia Toolforge wiki information
            toolforgeWikiProvider = new ToolforgeWikiProvider(cpds);

            // Use database credentials to create a secret prefix for caches
            final String redisSecret = DigestUtils.sha256Hex(configMyCnf.getUser() + ':' + configMyCnf.getPassword());

            final String redisApiCacheKeyPrefix = redisSecret + "-vcat-cache-api-";
            final String redisMetadataCacheKeyPrefix = redisSecret + "-vcat-cache-metadata-";

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
            final JedisPool jedisPool = new JedisPool(poolConfig, redisHostname, redisPort);

            // Use Redis for API and metadata caches
            final IApiCache apiCache = new ApiRedisCache(jedisPool, redisApiCacheKeyPrefix, cachePurge);
            final IMetadataCache metadataCache = new MetadataRedisCache(jedisPool, redisMetadataCacheKeyPrefix,
                    cachePurgeMetadata);

            final CachedApiClient<ToolforgeWiki> apiClient = new CachedApiClient<>(apiCache);

            // Metadata provider
            metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);

            // Home directory
            final Path homeDirectory;
            final String toolDataDir = System.getenv("TOOL_DATA_DIR");
            if (toolDataDir != null) {
                // Primary: from $TOOL_DATA_DIR (within container built by Build Service)
                homeDirectory = Paths.get(toolDataDir);
            } else {
                // Secondary: from $HOME
                homeDirectory = Paths.get(System.getProperty("user.home"));
            }

            // For cache of Graphviz files and rendered images, use this directory
            final Path cacheDir = homeDirectory.resolve(homeCacheDir);
            // Temporary directory for Graphviz files and rendered images
            final Path tempDir = homeDirectory.resolve(homeTempDir);

            Files.createDirectories(cacheDir);
            Files.createDirectories(tempDir);

            // Use gridserver to render Graphviz files
            final Graphviz graphviz = new QueuedGraphviz(
                    new GraphvizGridClient(jedisPool, redisSecret, homeDirectory.resolve("bin"), Paths.get(graphvizDir)),
                    graphvizThreads);

            // Create renderer
            vCatRenderer = new QueuedVCatRenderer<>(new CachedVCatRenderer<>(graphviz, tempDir, apiClient, cacheDir),
                    vcatThreads);

        } catch (IOException | VCatException e) {
            throw new ServletException(e);
        }

    }

    @Override
    protected RenderedFileInfo renderedFileFromRequest(final HttpServletRequest req) throws ServletException {

        if (vCatRenderer.getNumberOfQueuedJobs() > vcatQueue) {
            throw new ServletException(Messages.getString("ToolforgeVCatServlet.Error.TooManyQueuedJobs"));
        }

        Map<String, String[]> parameterMap;
        if (req.getRequestURI().endsWith(CATGRAPH_REDIRECT_URL_PATTERN)) {
            // If called as catgraphRedirect, convert from Catgraph parameters to vCat parameters
            parameterMap = CatgraphConverter.convertParameters(req.getParameterMap());
        } else {
            parameterMap = req.getParameterMap();
        }

        try {
            return vCatRenderer.render(new AllParamsToolforge(parameterMap, this.getHttpRequestURI(req),
                    metadataProvider, toolforgeWikiProvider));
        } catch (VCatException e) {
            throw new ServletException(e);
        }

    }

}
