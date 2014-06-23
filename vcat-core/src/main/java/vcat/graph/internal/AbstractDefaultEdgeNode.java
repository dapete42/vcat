package vcat.graph.internal;

import vcat.graph.Graph;

/**
 * Base class for all edges and nodes in a {@link Graph}.
 * 
 * @author Peter Schlömer
 */
public abstract class AbstractDefaultEdgeNode extends AbstractGraphPropertyUser {

	/** Label font family name. */
	private String fontname;

	/** Label size */
	private int fontsize;

	/** URL in imagemap, PostScript and SVG files. */
	private String href;

	private String style;

	/** @return Label font family name. */
	@GraphProperty("fontname")
	public String getFontname() {
		return this.fontname;
	}

	/**
	 * @return Label size.
	 */
	public int getFontsize() {
		return this.fontsize;
	}

	@GraphProperty(Graph.PROPERTY_FONTSIZE)
	public String getFontsizeString() {
		if (this.fontsize == 0) {
			return null;
		} else {
			return Integer.toString(this.fontsize);
		}
	}

	/**
	 * @return Label href attribute.
	 */
	@GraphProperty(Graph.PROPERTY_HREF)
	public String getHref() {
		return this.href;
	}

	/**
	 * @return Label style attribute.
	 */
	@GraphProperty(Graph.PROPERTY_STYLE)
	public String getStyle() {
		return this.style;
	}

	/**
	 * @param fontname
	 *            Label font family name.
	 */
	public void setFontname(String fontname) {
		this.fontname = fontname;
	}

	/**
	 * @param fontsize
	 *            Label size.
	 */
	public void setFontsize(int fontsize) {
		this.fontsize = fontsize;
	}

	/**
	 * @param href
	 *            Label href attribute.
	 */
	public void setHref(String href) {
		this.href = href;
	}

	/**
	 * @param style
	 *            Label style attribute.
	 */
	public void setStyle(String style) {
		this.style = style;
	}

}
