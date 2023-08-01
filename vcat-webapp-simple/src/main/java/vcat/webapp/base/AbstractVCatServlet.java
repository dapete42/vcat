package vcat.webapp.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vcat.renderer.RenderedFileInfo;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Abstract base class for Servlets that return rendered VCat graphs.
 *
 * @author Peter Schlömer
 */
@SuppressWarnings("serial")
public abstract class AbstractVCatServlet extends HttpServlet {

    /**
     * Log4j2 Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVCatServlet.class);

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        this.doRequest(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        this.doRequest(req, resp);
    }

    protected void doRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            RenderedFileInfo renderedFileInfo = this.renderedFileFromRequest(req);

            // Get finished rendered file
            Path resultFile = renderedFileInfo.getFile();

            // Content-type
            String contentType = renderedFileInfo.getMimeType();
            resp.setContentType(contentType);

            // Content-length
            long length = Files.size(resultFile);
            if (length < Integer.MAX_VALUE) {
                resp.setContentLength((int) length);
            }

            // Content-disposition (for file name)
            String filename = resultFile.getFileName().toString();
            resp.setHeader("Content-disposition", "filename=\"" + filename + '"');

            // Serve file to browser
            try (InputStream renderedInput = Files.newInputStream(resultFile);
                 ServletOutputStream output = resp.getOutputStream()) {
                renderedInput.transferTo(output);
            }

            LOGGER.info("File sent: '{}' sent as '{}', {} bytes", resultFile.toAbsolutePath(), contentType, length);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected abstract RenderedFileInfo renderedFileFromRequest(HttpServletRequest req) throws ServletException;

}
