package vcat.graphviz;

import java.io.File;
import java.util.regex.Pattern;

import vcat.params.GraphvizParams;

/**
 * Interface for a generic Graphviz renderer.
 * 
 * @author Peter Schlömer
 */
public interface Graphviz {

	/**
	 * Pattern do determine if a string can be used as an identifier in a graphviz file without quoting.
	 */
	public final static Pattern PATTERN_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]+");

	public final static String EDGE = "edge";

	public final static String NODE = "node";

	public final static String PROPERTY_FONTNAME = "fontname";

	public final static String PROPERTY_FONTSIZE = "fontsize";

	public final static String PROPERTY_HREF = "href";

	public final static String PROPERTY_LABEL = "label";

	public final static String PROPERTY_RANK = "rank";

	public final static String PROPERTY_SHAPE = "shape";

	public final static String PROPERTY_SPLINES = "splines";

	public final static String PROPERTY_STYLE = "style";

	public final static String SHAPE_RECT = "rect";

	public final static String STYLE_BOLD = "bold";

	public final static String STYLE_DASHED = "dashed";

	public final static String TRUE = "true";

	public abstract void render(File inputFile, File outputFile, GraphvizParams params) throws GraphvizException;

}