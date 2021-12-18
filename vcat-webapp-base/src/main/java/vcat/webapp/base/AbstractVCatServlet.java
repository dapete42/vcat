package vcat.webapp.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vcat.renderer.RenderedFileInfo;

/**
 * Abstract base class for Servlets that return rendered VCat graphs.
 * 
 * @author Peter Schlömer
 */
@SuppressWarnings("serial")
public abstract class AbstractVCatServlet extends HttpServlet {

	/** Log4j2 Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVCatServlet.class);

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
			RenderedFileInfo renderedFileInfo = this.renderedFileFromRequest(req);

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

			LOGGER.info(String.format("File sent: '%s' sent as '%s', %d bytes", resultFile.getAbsolutePath(),
					contentType, length));
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	protected abstract RenderedFileInfo renderedFileFromRequest(HttpServletRequest req) throws ServletException;

}
