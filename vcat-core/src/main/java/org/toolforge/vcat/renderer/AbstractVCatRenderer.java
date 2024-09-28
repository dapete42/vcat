package org.toolforge.vcat.renderer;

import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.params.AbstractAllParams;
import org.toolforge.vcat.params.Links;
import org.toolforge.vcat.params.OutputFormat;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;

import java.io.Serial;
import java.nio.file.Path;

public abstract class AbstractVCatRenderer implements VCatRenderer {

    @Serial
    private static final long serialVersionUID = 6181737937496899531L;

    protected abstract Path createGraphFile(AbstractAllParams all) throws VCatException;

    protected abstract Path createImagemapHtmlFile(AbstractAllParams all, OutputFormat imageFormat)
            throws VCatException;

    protected Path createRenderedFile(AbstractAllParams all) throws VCatException {
        final Path graphFile = createGraphFile(all);
        return createRenderedFileFromGraphFile(all, graphFile);
    }

    protected abstract Path createRenderedFileFromGraphFile(AbstractAllParams all, Path graphFile)
            throws VCatException;

    @Override
    public RenderedFileInfo render(final AbstractAllParams all) throws VCatException {
        // Get and, if necessary, create result file
        final Path resultFile;
        var outputFormat = all.getGraphviz().getOutputFormat();
        if (outputFormat == OutputFormat.GraphvizRaw) {
            // GraphvizRaw returns just the graph file
            resultFile = createGraphFile(all);
        } else if (all.getVCat().getLinks() != Links.None && outputFormat.hasImageMapOutputFormat()) {
            // If links are requested, some formats want to be shown in an HTML page with an image map. This means
            // the output format has to be changed to an internal value, but the original format is needed as well.
            final OutputFormat imageFormat = outputFormat;
            outputFormat = imageFormat.getImageMapOutputFormat();
            all.getGraphviz().setOutputFormat(outputFormat);
            resultFile = createImagemapHtmlFile(all, imageFormat);
        } else {
            resultFile = createRenderedFile(all);
        }

        return new RenderedFileInfo(resultFile, outputFormat.getMimeType());
    }

}
