package vcat.toollabs.webapp;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class ToollabsCatgraphServlet extends ToollabsVCatServlet {

	private static final long serialVersionUID = -1014810291958747510L;

	@Override
	public String getServletInfo() {
		return Messages.getString("ToollabsCatgraphServlet.ServletInfo");
	}

	@Override
	protected Map<String, String[]> parameterMap(final HttpServletRequest req) {
		// Convert Graphviz parameters to vCat parameters
		return CatgraphConverter.convertParameters(req.getParameterMap());
	}

}
