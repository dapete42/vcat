package org.toolforge.vcat.toolforge.webapp;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.toolforge.vcat.toolforge.webapp.beans.ErrorBean;

import java.io.IOException;
import java.io.Serial;

@WebServlet(urlPatterns = "/catgraphConvert")
public class ToolforgeCatgraphConverterServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 6625255065147215965L;

    @Inject
    private transient ErrorBean errorBean;

    private void doRequest(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.getRequestDispatcher("catgraphConverter.xhtml").forward(req, resp);
        } catch (Exception e) {
            errorBean.setException(e);
            req.getRequestDispatcher("error.xhtml").forward(req, resp);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doRequest(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doRequest(req, resp);
    }

    @Override
    public String getServletInfo() {
        return Messages.getString("ToolforgeCatgraphConverterServlet.ServletInfo");
    }

}
