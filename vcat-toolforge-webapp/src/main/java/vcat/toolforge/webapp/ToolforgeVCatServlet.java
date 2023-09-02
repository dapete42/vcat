package vcat.toolforge.webapp;

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
import vcat.graphviz.GraphvizExternal;
import vcat.graphviz.QueuedGraphviz;
import vcat.mediawiki.CachedApiClient;
import vcat.mediawiki.CachedMetadataProvider;
import vcat.mediawiki.IMetadataProvider;
import vcat.redis.cache.ApiRedisCache;
import vcat.redis.cache.MetadataRedisCache;
import vcat.renderer.CachedVCatRenderer;
import vcat.renderer.QueuedVCatRenderer;
import vcat.renderer.RenderedFileInfo;

import javax.sql.DataSource;
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

    /**
     * Injected Quarkus data source
     */
    @Inject
    DataSource dataSource;

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

            // Provider for Wikimedia Toolforge wiki information
            toolforgeWikiProvider = new ToolforgeWikiProvider(dataSource);

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

            // For cache of Graphviz files and rendered images, use this directory
            final Path cacheDir = Files.createTempDirectory("vcat-cache");
            // Temporary directory for Graphviz files and rendered images
            final Path tempDir = Files.createTempDirectory("vcat-temp");

            Files.createDirectories(cacheDir);
            Files.createDirectories(tempDir);

            final Path graphvizDirPath = Paths.get(graphvizDir);
            final Graphviz baseGraphviz = new GraphvizExternal(graphvizDirPath);
            final Graphviz graphviz = new QueuedGraphviz(baseGraphviz, graphvizThreads);

            // Create renderer
            vCatRenderer = new QueuedVCatRenderer<>(
                    new CachedVCatRenderer<>(graphviz, tempDir, apiClient, cacheDir, cachePurge),
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
