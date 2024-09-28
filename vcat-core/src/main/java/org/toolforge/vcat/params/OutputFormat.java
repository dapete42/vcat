package org.toolforge.vcat.params;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Output format for vCat.
 *
 * @author Peter Schlömer
 */
@Getter
public enum OutputFormat {

    /**
     * Image map fragment (<code>&lt;map&gt;</code> element). For internal use only.
     */
    _Imagemap("cmapx", "cmapx", null, null),
    /**
     * HTML page with image map and GIF image. For internal use only.
     */
    _HTMLGIF("gif.html", null, "text/html; charset=UTF-8", null),
    /**
     * HTML page with image map and PNG image. For internal use only.
     */
    _HTMLPNG("png.html", null, "text/html; charset=UTF-8", null),

    /**
     * Raw Graphviz source, not passed through Graphviz.
     */
    GraphvizRaw("gv", null, "text/plain; charset=UTF-8", null, "dot", "gv"),
    /**
     * GIF image (force rendering with cairo).
     */
    GIF("gif", "gif", "image/gif", OutputFormat._HTMLGIF, "gif"),
    /**
     * PDF.
     */
    PDF("pdf", "pdf", "application/pdf", null, "pdf"),
    /**
     * PNG image (force rendering with cairo).
     */
    PNG("png", "png", "image/png", OutputFormat._HTMLPNG, "png"),
    /**
     * SVG image.
     */
    SVG("svg", "svg", "image/svg+xml; charset=UTF-8", null, "svg");

    @Nullable
    public static OutputFormat valueOfIgnoreCase(String name) {
        for (OutputFormat format : values()) {
            for (String parameterName : format.parameterNames) {
                if (parameterName.equalsIgnoreCase(name)) {
                    return format;
                }
            }
        }
        return null;
    }

    private final String fileExtension;

    @Nullable
    private final String graphvizTypeParameter;

    @Nullable
    private final OutputFormat imageMapOutputFormat;

    @Nullable
    private final String mimeType;

    private final String[] parameterNames;

    /**
     * @param fileExtension         File extension for the output format.
     * @param graphvizTypeParameter Type parameter to pass when calling the graphviz command line tool.
     * @param mimeType              MIME type for the output format.
     * @param imageMapOutputFormat  If not null, this output format has a special output format when links are included; this is used to
     *                              create an image map and an HTML page containing it.
     * @param parameterNames        Names this format should be recognized by when calling {@link #valueOfIgnoreCase(String)}.
     */
    OutputFormat(String fileExtension, @Nullable String graphvizTypeParameter, @Nullable String mimeType, @Nullable OutputFormat imageMapOutputFormat,
                 String... parameterNames) {
        this.fileExtension = fileExtension;
        this.graphvizTypeParameter = graphvizTypeParameter;
        this.mimeType = mimeType;
        this.imageMapOutputFormat = imageMapOutputFormat;
        this.parameterNames = parameterNames;
    }

    /**
     * @return Whether to use another output format when creating an image map page to support links in static images.
     */
    public boolean hasImageMapOutputFormat() {
        return imageMapOutputFormat != null;
    }

}
