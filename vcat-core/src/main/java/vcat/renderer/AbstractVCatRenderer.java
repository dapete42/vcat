package vcat.renderer;

import vcat.VCatException;
import vcat.mediawiki.interfaces.Wiki;
import vcat.params.AbstractAllParams;
import vcat.params.Links;
import vcat.params.OutputFormat;
import vcat.renderer.interfaces.VCatRenderer;

import java.io.Serial;
import java.nio.file.Path;

public abstract class AbstractVCatRenderer<W extends Wiki> implements VCatRenderer<W> {

    @Serial
    private static final long serialVersionUID = 6181737937496899531L;

    protected abstract Path createGraphFile(final AbstractAllParams<W> all) throws VCatException;

    protected abstract Path createImagemapHtmlFile(final AbstractAllParams<W> all, final OutputFormat imageFormat)
            throws VCatException;

    protected Path createRenderedFile(final AbstractAllParams<W> all) throws VCatException {
        final Path graphFile = this.createGraphFile(all);
        return this.createRenderedFileFromGraphFile(all, graphFile);
    }

    protected abstract Path createRenderedFileFromGraphFile(final AbstractAllParams<W> all, final Path graphFile)
            throws VCatException;

    @Override
    public RenderedFileInfo render(final AbstractAllParams<W> all) throws VCatException {
        // Get and, if necessary, create result file
        final Path resultFile;
        OutputFormat outputFormat = all.getGraphviz().getOutputFormat();
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