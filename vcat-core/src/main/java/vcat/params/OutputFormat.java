package vcat.params;

/**
 * Output format for vCat.
 * 
 * @author Peter Schlömer
 */
public enum OutputFormat {

	/** Raw Graphviz source, which not passed through Graphviz. */
	GraphvizRaw("gv", null, "text/plain; charset=UTF-8", "dot", "gv"),
	/** GIF image (force rendering with cairo). */
	GIF("gif", "gif:cairo", "image/gif", "gif"),
	/** PDF. */
	PDF("pdf", "pdf", "application/pdf", "pdf"),
	/** PNG image (force rendering with cairo). */
	PNG("png", "png:cairo", "image/png", "png"),
	/** SVG image. */
	SVG("svg", "svg", "image/svg+xml; charset=UTF-8", "svg");

	public static OutputFormat valueOfIgnoreCase(String name) {
		for (OutputFormat format : values()) {
			if (format.parameterNames != null) {
				for (int i = 0; i < format.parameterNames.length; i++) {
					if (format.parameterNames[i].equalsIgnoreCase(name)) {
						return format;
					}
				}
			}
		}
		return null;
	}

	private final String fileExtension;

	private final String graphvizTypeParameter;

	private final String mimeType;

	private final String[] parameterNames;

	OutputFormat(String fileExtension, String graphvizTypeParameter, String mimeType, String... parameterNames) {
		this.fileExtension = fileExtension;
		this.graphvizTypeParameter = graphvizTypeParameter;
		this.mimeType = mimeType;
		this.parameterNames = parameterNames;
	}

	/**
	 * @return File extension for the output format.
	 */
	public String getFileExtension() {
		return this.fileExtension;
	}

	/**
	 * @return Type parameter to pass when calling the graphviz command line too.
	 */
	public String getGraphvizTypeParameter() {
		return this.graphvizTypeParameter;
	}

	/**
	 * @return MIME type for the output format.
	 */
	public String getMimeType() {
		return this.mimeType;
	}

	/**
	 * @return Names this format should be recognized by when calling {@link #valueOfIgnoreCase(String)}.
	 */
	public String[] getParameterNames() {
		return this.parameterNames;
	}

}
