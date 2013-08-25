package vcat.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.VCatException;
import vcat.VCatRenderer;
import vcat.VCatRenderer.RenderedFileInfo;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.cache.file.ApiFileCache;
import vcat.cache.file.MetadataFileCache;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.graphviz.GraphvizJNI;

public class VCatServlet extends HttpServlet {

	private static final long serialVersionUID = 7874314791561296530L;

	private static final Log log = LogFactory.getLog(VCatServlet.class);

	private static int PURGE = 600;

	private static int PURGE_METADATA = 86400;

	private static final File TMP_DIR = new File("/tmp/vcat");

	private VCatRenderer vCatRenderer;

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
			RenderedFileInfo renderedFileInfo = this.vCatRenderer.render(req.getParameterMap());

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
			resp.setHeader("Content-disposition", "filename=\"" + filename + "\"");

			// Serve file to browser
			try (FileInputStream renderedInput = new FileInputStream(resultFile);
					ServletOutputStream output = resp.getOutputStream()) {
				IOUtils.copy(renderedInput, output);
			}

			log.info("File sent: '" + resultFile.getAbsolutePath() + "' sent as '" + contentType + "', " + length
					+ " bytes");
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	public String getServletInfo() {
		return "vCat Servlet";
	}

	@Override
	public void init() throws ServletException {
		try {
			final Graphviz graphviz = new GraphvizJNI();
			final IApiCache apiCache = new ApiFileCache(new File(TMP_DIR, "api"), PURGE);
			final IMetadataCache metadataCache = new MetadataFileCache(new File(TMP_DIR, "metadata"), PURGE_METADATA);
			this.vCatRenderer = new VCatRenderer(graphviz, TMP_DIR, apiCache, metadataCache, PURGE);
		} catch (CacheException | GraphvizException | VCatException e) {
			throw new ServletException(e);
		}
	}

}
