package vcat.toolforge.webapp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import vcat.VCatException;
import vcat.caffeine.ApiCaffeineCache;
import vcat.caffeine.MetadataCaffeineCache;
import vcat.graphviz.GraphvizExternal;
import vcat.graphviz.QueuedGraphviz;
import vcat.mediawiki.CachedApiClient;
import vcat.mediawiki.CachedMetadataProvider;
import vcat.mediawiki.IMetadataProvider;
import vcat.renderer.CachedVCatRenderer;
import vcat.renderer.QueuedVCatRenderer;
import vcat.renderer.RenderedFileInfo;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
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

            final var configMyCnf = new MyCnfConfig();
            configMyCnf.readFromMyCnf();

            // Provider for Wikimedia Toolforge wiki information
            toolforgeWikiProvider = new ToolforgeWikiProvider(dataSource);

            // Use Caffeine for API and metadata caches
            final var apiCache = new ApiCaffeineCache(10000, cachePurge);
            final var metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);

            final CachedApiClient<ToolforgeWiki> apiClient = new CachedApiClient<>(apiCache);

            // Metadata provider
            metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);

            // For cache of Graphviz files and rendered images, use this directory
            final var cacheDir = Files.createTempDirectory("vcat-cache");
            // Temporary directory for Graphviz files and rendered images
            final var tempDir = Files.createTempDirectory("vcat-temp");

            Files.createDirectories(cacheDir);
            Files.createDirectories(tempDir);

            final var graphvizDirPath = Paths.get(graphvizDir);
            final var baseGraphviz = new GraphvizExternal(graphvizDirPath);
            final var graphviz = new QueuedGraphviz(baseGraphviz, graphvizThreads);

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

        final Map<String, String[]> parameterMap;
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
