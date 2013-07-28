package vcat.params;

/**
 * Output format for vCat.
 * 
 * @author Peter Schlömer
 */
public enum OutputFormat {

	/** Raw Graphviz source, which not passed through Graphviz. */
	GraphvizRaw("gv", null, "text/plain; charset=UTF-8"),
	/** GIF image (force rendering with cairo). */
	GIF("gif", "gif:cairo", "image/gif"),
	/** PDF. */
	PDF("pdf", "pdf", "application/pdf"),
	/** PNG image (force rendering with cairo). */
	PNG("png", "png:cairo", "image/png"),
	/** SVG image. */
	SVG("svg", "svg", "image/svg+xml; charset=UTF-8");

	public static OutputFormat valueOfIgnoreCase(String name) {
		for (OutputFormat format : values()) {
			if (format.name().equalsIgnoreCase(name)) {
				return format;
			}
		}
		return null;
	}

	private String fileExtension;

	private String graphvizTypeParameter;

	private String mimeType;

	OutputFormat(String fileExtension, String graphvizTypeParameter, String mimeType) {
		this.fileExtension = fileExtension;
		this.graphvizTypeParameter = graphvizTypeParameter;
		this.mimeType = mimeType;
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

}
