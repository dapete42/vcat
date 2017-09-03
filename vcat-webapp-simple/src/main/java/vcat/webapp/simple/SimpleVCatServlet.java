package vcat.webapp.simple;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import vcat.VCatException;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.cache.file.ApiFileCache;
import vcat.cache.file.MetadataFileCache;
import vcat.graphviz.GraphvizExternal;
import vcat.graphviz.QueuedGraphviz;
import vcat.mediawiki.CachedApiClient;
import vcat.mediawiki.CachedMetadataProvider;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IMetadataProvider;
import vcat.mediawiki.SimpleWikimediaWiki;
import vcat.params.AllParams;
import vcat.renderer.CachedVCatRenderer;
import vcat.renderer.IVCatRenderer;
import vcat.renderer.QueuedVCatRenderer;
import vcat.renderer.RenderedFileInfo;
import vcat.webapp.base.AbstractVCatServlet;

@WebServlet(urlPatterns = "/render")
public class SimpleVCatServlet extends AbstractVCatServlet {

	private static final long serialVersionUID = 8952525278394777354L;

	private static int PURGE = 600;

	private static int PURGE_METADATA = 86400;

	private ICategoryProvider<SimpleWikimediaWiki> categoryProvider;

	private IMetadataProvider metadataProvider;

	private IVCatRenderer<SimpleWikimediaWiki> vCatRenderer;

	@Override
	public String getServletInfo() {
		return "Simple vCat servlet";
	}

	@Override
	public void init() throws ServletException {
		final File cacheDir = new File(Config.getString(Config.CONFIG_CACHEDIR));
		final File apiDir = new File(cacheDir, "api");
		final File metadataDir = new File(cacheDir, "metadata");
		final File tempDir = (File) this.getServletContext().getAttribute(ServletContext.TEMPDIR);
		apiDir.mkdirs();
		metadataDir.mkdirs();
		try {
			final QueuedGraphviz graphviz = new QueuedGraphviz(new GraphvizExternal(new File("/usr/bin")), 1);
			final IApiCache apiCache = new ApiFileCache(apiDir, PURGE);
			final CachedApiClient<SimpleWikimediaWiki> apiClient = new CachedApiClient<>(apiCache);
			this.categoryProvider = apiClient;
			final IMetadataCache metadataCache = new MetadataFileCache(metadataDir, PURGE_METADATA);
			this.metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);
			this.vCatRenderer = new QueuedVCatRenderer<>(
					new CachedVCatRenderer<>(graphviz, tempDir, this.categoryProvider, cacheDir, PURGE), 10);
		} catch (CacheException | VCatException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected RenderedFileInfo renderedFileFromRequest(final HttpServletRequest req) throws ServletException {
		try {
			return this.vCatRenderer
					.render(new AllParams(req.getParameterMap(), req.getRequestURI(), this.metadataProvider));
		} catch (VCatException e) {
			throw new ServletException(e);
		}
	}

}
