package vcat.toolforge.webapp;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vcat.VCatException;
import vcat.renderer.RenderedFileInfo;
import vcat.toolforge.webapp.beans.ErrorBean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractVCatToolforgeServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 2278840722430941495L;

    /**
     * SLF4J Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVCatToolforgeServlet.class);

    @Inject
    private transient ErrorBean errorBean;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doRequest(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doRequest(req, resp);
    }

    private void doRequestInternal(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
                IOUtils.copy(renderedInput, output);
            }

            LOG.info("File sent: '{}' sent as '{}', {} bytes", resultFile.toAbsolutePath(), contentType, length);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected abstract RenderedFileInfo renderedFileFromRequest(HttpServletRequest req) throws ServletException;

    protected void doRequest(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // Add a wrapper around doRequest which displays a nice error page instead of the default error message.
        try {
            doRequestInternal(req, resp);
        } catch (IOException | ServletException e) {
            if (!(e.getCause() instanceof VCatException)) {
                LOG.error(e.getMessage(), e);
            }
            errorBean.setException(e);
            req.getRequestDispatcher("error.xhtml").forward(req, resp);
        }
    }

    protected String getHttpRequestURI(final HttpServletRequest req) {
        final String requestURI = req.getRequestURI();
        if (requestURI.startsWith("https")) {
            return "http" + requestURI.substring(5);
        } else {
            return requestURI;
        }
    }

}
