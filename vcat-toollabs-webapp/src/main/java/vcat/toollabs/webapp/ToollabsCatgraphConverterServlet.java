package vcat.toollabs.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ToollabsCatgraphConverterServlet extends HttpServlet {

	private static final long serialVersionUID = 6874523244125197180L;

	private static void doRequest(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			final Map<String, String[]> requestParams = req.getParameterMap();

			final String lang = requestParams.containsKey("lang") ? requestParams.get("lang")[0] : "en";
			req.setAttribute("lang", lang);

			if (requestParams.containsKey("doConvert")) {
				final String inputUrl = requestParams.get("inputUrl")[0];
				final int queryStart = inputUrl.indexOf('?');
				final String urlParameters = inputUrl.substring(queryStart + 1);
				final HashMap<String, String[]> inputParameters = new HashMap<>();
				for (String nameValueString : urlParameters.split("&")) {
					final String[] split = nameValueString.split("=");
					final String name = split[0];
					String value = null;
					if (split.length > 1) {
						value = split[1];
					}
					if (inputParameters.containsKey(name)) {
						final String[] oldValues = inputParameters.get(name);
						final String[] newValues = Arrays.copyOf(oldValues, oldValues.length + 1);
						newValues[oldValues.length] = value;
						inputParameters.put(name, newValues);
					} else {
						inputParameters.put(name, new String[] { value });
					}
				}

				final Map<String, String[]> outputParameters = CatgraphConverter.convertParameters(inputParameters);

				// Build URL and list for output, only with known parameters
				final String[] sortList = new String[] { "wiki", "category", "title", "ns", "rel", "depth", "limit",
						"showhidden", "algorithm", "format", "links" };
				final List<String> outputParameterList = new ArrayList<>();
				for (String key : sortList) {
					final String[] values = outputParameters.get(key);
					if (values != null) {
						for (String value : values) {
							outputParameterList.add(key + '=' + value);
						}
					}
				}
				final String outputUrl = "http://tools.wmflabs.org/vcat/render?"
						+ StringUtils.join(outputParameterList, '&');

				req.setAttribute("hasResult", true);
				req.setAttribute("inputUrl", inputUrl);
				req.setAttribute("inputParameters", inputParameters);
				req.setAttribute("outputParameterList", outputParameterList);
				req.setAttribute("outputUrl", outputUrl);
			} else {
				req.setAttribute("inputUrl", "");
				req.setAttribute("hasResult", false);
			}
			req.getRequestDispatcher("WEB-INF/catgraphConverter.jsp").forward(req, resp);

		} catch (Exception e) {
			req.setAttribute("exceptionMessage", e.getMessage());
			req.setAttribute("stacktrace", ExceptionUtils.getStackTrace(e));
			req.getRequestDispatcher("WEB-INF/error.jsp").forward(req, resp);
		}
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
			IOException {
		doRequest(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
			IOException {
		doRequest(req, resp);
	}

	@Override
	public String getServletInfo() {
		return Messages.getString("ToollabsCatgraphConverterServlet.ServletInfo");
	}

}
