package org.toolforge.vcat.toolforge.webapp;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serial;

@WebServlet(urlPatterns = "/status")
public class StatusServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1401109273262439060L;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("status.xhtml").forward(req, resp);
    }

    @Override
    public String getServletInfo() {
        return "StatusServlet";
    }

}
