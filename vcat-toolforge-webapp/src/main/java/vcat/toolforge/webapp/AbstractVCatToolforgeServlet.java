package vcat.toolforge.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;

import vcat.VCatException;
import vcat.webapp.base.AbstractVCatServlet;

@SuppressWarnings("serial")
public abstract class AbstractVCatToolforgeServlet extends AbstractVCatServlet {

	@Override
	protected void doRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
			IOException {
		// Add a wrapper around doRequest which displays a nice error page instead of the default error message.
		try {
			super.doRequest(req, resp);
		} catch (IOException | ServletException e) {
			if (!(e.getCause() instanceof VCatException)) {
				log.error(e.getMessage(), e);
			}
			req.setAttribute("exceptionMessage", e.getMessage());
			req.setAttribute("stacktrace", ExceptionUtils.getStackTrace(e));
			req.getRequestDispatcher("WEB-INF/error.jsp").forward(req, resp);
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
