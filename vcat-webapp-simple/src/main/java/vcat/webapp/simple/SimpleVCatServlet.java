package vcat.webapp.simple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

	private static final int PURGE = 600;

	private static final int PURGE_METADATA = 86400;

	private static IMetadataProvider metadataProvider;

	private static IVCatRenderer<SimpleWikimediaWiki> vCatRenderer;

	@Override
	public String getServletInfo() {
		return "Simple vCat servlet";
	}

	@Override
	public void init() throws ServletException {
		final Path cacheDir = Paths.get(Config.getString(Config.CONFIG_CACHEDIR));
		final Path apiDir = cacheDir.resolve("api");
		final Path metadataDir = cacheDir.resolve("metadata");
		final Path tempDir = ((File) this.getServletContext().getAttribute(ServletContext.TEMPDIR)).toPath();
		try {
			final QueuedGraphviz graphviz = new QueuedGraphviz(new GraphvizExternal(Paths.get("/usr/bin")), 1);
			Files.createDirectories(apiDir);
			final IApiCache apiCache = new ApiFileCache(apiDir, PURGE);
			final CachedApiClient<SimpleWikimediaWiki> apiClient = new CachedApiClient<>(apiCache);
			Files.createDirectories(metadataDir);
			final IMetadataCache metadataCache = new MetadataFileCache(metadataDir, PURGE_METADATA);
			metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);
			vCatRenderer = new QueuedVCatRenderer<>(
					new CachedVCatRenderer<>(graphviz, tempDir, apiClient, cacheDir, PURGE), 10);
		} catch (CacheException | IOException | VCatException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected RenderedFileInfo renderedFileFromRequest(final HttpServletRequest req) throws ServletException {
		try {
			return vCatRenderer.render(new AllParams(req.getParameterMap(), req.getRequestURI(), metadataProvider));
		} catch (VCatException e) {
			throw new ServletException(e);
		}
	}

}
