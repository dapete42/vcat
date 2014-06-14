package vcat.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

public class VCatServlet extends HttpServlet {

	private static final long serialVersionUID = -8091085002046525690L;

	private final Log log = LogFactory.getLog(this.getClass());

	private static int PURGE = 600;

	private static int PURGE_METADATA = 86400;

	private ICategoryProvider<SimpleWikimediaWiki> categoryProvider;

	private IMetadataProvider metadataProvider;

	private IVCatRenderer<SimpleWikimediaWiki> vCatRenderer;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doRequest(req, resp);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doRequest(req, resp);
	}

	protected void doRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {

			final AllParams all = new AllParams(req.getParameterMap(), req.getRequestURI(), this.metadataProvider);
			RenderedFileInfo renderedFileInfo = this.vCatRenderer.render(all);

			// Get finished rendered file
			File resultFile = renderedFileInfo.getFile();

			// Content-type
			String contentType = renderedFileInfo.getMimeType();
			resp.setContentType(contentType);

			// Content-length
			long length = resultFile.length();
			if (length < Integer.MAX_VALUE) {
				resp.setContentLength((int) length);
			}

			// Content-disposition (for file name)
			String filename = resultFile.getName();
			resp.setHeader("Content-disposition", "filename=\"" + filename + '"');

			// Serve file to browser
			try (FileInputStream renderedInput = new FileInputStream(resultFile);
					ServletOutputStream output = resp.getOutputStream()) {
				IOUtils.copy(renderedInput, output);
			}

			log.info(String.format(Messages.getString("VCatServlet.Info.FileSent"), resultFile.getAbsolutePath(),
					contentType, length));
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	public String getServletInfo() {
		return Messages.getString("VCatServlet.ServletInfo");
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
			this.vCatRenderer = new QueuedVCatRenderer<>(new CachedVCatRenderer<>(graphviz, tempDir,
					this.categoryProvider, cacheDir, PURGE), 10);
		} catch (CacheException | VCatException e) {
			throw new ServletException(e);
		}
	}
}
