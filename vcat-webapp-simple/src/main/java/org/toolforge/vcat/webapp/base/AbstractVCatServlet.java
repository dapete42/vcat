package org.toolforge.vcat.webapp.base;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.renderer.RenderedFileInfo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Abstract base class for Servlets that return rendered VCat graphs.
 *
 * @author Peter Schlömer
 */
@Slf4j
@SuppressWarnings("serial")
public abstract class AbstractVCatServlet extends HttpServlet {

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

            LOG.info("File sent: '{}' sent as '{}', {} bytes", resultFile.toAbsolutePath(), contentType, length);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected abstract RenderedFileInfo renderedFileFromRequest(HttpServletRequest req) throws ServletException;

}
