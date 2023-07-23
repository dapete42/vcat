package vcat.toolforge.webapp;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import vcat.VCatException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.graphviz.Graphviz;
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

@WebServlet(urlPatterns = {"/render", ToolforgeVCatServlet.CATGRAPH_REDIRECT_URL_PATTERN})
public class ToolforgeVCatServlet extends AbstractVCatToolforgeServlet {

    @Serial
    private static final long serialVersionUID = -5655389767357096359L;

    protected static final String CATGRAPH_REDIRECT_URL_PATTERN = "/catgraphRedirect";

    /**
     * Directory with Graphviz binaries (dot, fdp).
     */
    private static final String GRAPHVIZ_DIR = "/usr/bin";

    private static final String HOME_CACHE_DIR = "work/cache";

    private static final String HOME_TEMP_DIR = "work/temp";

    /**
     * JDBC URL for MySQL/MariaDB access to wiki table.
     */
    private static final String JDBC_URL = "jdbc:mariadb://s7.web.db.svc.wikimedia.cloud:3306/meta_p";

    /**
     * Purge caches after (seconds).
     */
    private static final int PURGE = 600;

    /**
     * Purge metadata after (seconds).
     */
    private static final int PURGE_METADATA = 86400;

    /**
     * Redis server hostname.
     */
    private static final String REDIS_HOSTNAME = "tools-redis";

    /**
     * Redis server port.
     */
    private static final int REDIS_PORT = 6379;

    /**
     * Maximum number of concurrent threads running vCat (0=unlimited).
     */
    private static final int VCAT_THREADS = 4;

    private static final int VCAT_MAX_QUEUE = 20;

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
            cpds.setJdbcUrl(JDBC_URL);
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
            final JedisPool jedisPool = new JedisPool(poolConfig, REDIS_HOSTNAME, REDIS_PORT);

            // Use Redis for API and metadata caches
            final IApiCache apiCache = new ApiRedisCache(jedisPool, redisApiCacheKeyPrefix, PURGE);
            final IMetadataCache metadataCache = new MetadataRedisCache(jedisPool, redisMetadataCacheKeyPrefix,
                    PURGE_METADATA);

            final CachedApiClient<ToolforgeWiki> apiClient = new CachedApiClient<>(apiCache);

            // Metadata provider
            metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);

            // Home directory
            final Path homeDirectory = Paths.get(System.getProperty("user.home"));

            // For cache of Graphviz files and rendered images, use this directory
            final Path cacheDir = homeDirectory.resolve(HOME_CACHE_DIR);
            // Temporary directory for Graphviz files and rendered images
            final Path tempDir = homeDirectory.resolve(HOME_TEMP_DIR);

            Files.createDirectories(cacheDir);
            Files.createDirectories(tempDir);

            // Use gridserver to render Graphviz files. The vCat is already queued, so this one does not have to be.
            final Graphviz graphviz = new GraphvizGridClient(jedisPool, redisSecret, homeDirectory.resolve("bin"),
                    Paths.get(GRAPHVIZ_DIR));

            // Create renderer
            vCatRenderer = new QueuedVCatRenderer<>(new CachedVCatRenderer<>(graphviz, tempDir, apiClient, cacheDir),
                    VCAT_THREADS);

        } catch (IOException | VCatException e) {
            throw new ServletException(e);
        }

    }

    @Override
    protected RenderedFileInfo renderedFileFromRequest(final HttpServletRequest req) throws ServletException {

        if (vCatRenderer.getNumberOfQueuedJobs() > VCAT_MAX_QUEUE) {
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
