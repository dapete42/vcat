package vcat.renderer;

import java.io.File;

import vcat.VCatException;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.params.Links;
import vcat.params.OutputFormat;

public abstract class AbstractVCatRenderer<W extends IWiki> {

	protected abstract File createGraphFile(final AbstractAllParams<W> all) throws VCatException;

	protected abstract File createImagemapHtmlFile(final AbstractAllParams<W> all, final OutputFormat imageFormat)
			throws VCatException;

	protected File createRenderedFile(final AbstractAllParams<W> all) throws VCatException {
		final File graphFile = this.createGraphFile(all);
		return this.createRenderedFileFromGraphFile(all, graphFile);
	}

	protected abstract File createRenderedFileFromGraphFile(final AbstractAllParams<W> all, final File graphFile)
			throws VCatException;

	public RenderedFileInfo render(final AbstractAllParams<W> all) throws VCatException {
		// Get and, if necessary, create result file
		final File resultFile;
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