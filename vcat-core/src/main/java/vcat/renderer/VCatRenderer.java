package vcat.renderer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;

import vcat.AbstractVCat;
import vcat.VCatException;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.params.OutputFormat;
import vcat.params.VCatFactory;

public class VCatRenderer<W extends IWiki> extends AbstractVCatRenderer<W> {

	private final Graphviz graphviz;

	private final File outputDir;

	private final VCatFactory<W> vCatFactory;

	public VCatRenderer(final Graphviz graphviz, final File tempDir, final ICategoryProvider<W> categoryProvider)
			throws VCatException {
		this.graphviz = graphviz;
		this.outputDir = tempDir;
		this.vCatFactory = new VCatFactory<>(categoryProvider);
	}

	@Override
	protected File createGraphFile(final AbstractAllParams<W> all) throws VCatException {
		final AbstractVCat<W> vCat = this.vCatFactory.createInstance(all);
		try {
			final File outputFile = File.createTempFile("temp-Graph-", '.' + all.getGraphviz().getOutputFormat()
					.getFileExtension(), this.outputDir);
			vCat.renderToFile(outputFile);
			return outputFile;
		} catch (GraphvizException | IOException e) {
			throw new VCatException(e);
		}
	}

	@Override
	protected File createImagemapHtmlFile(final AbstractAllParams<W> all, final OutputFormat imageFormat)
			throws VCatException {

		final OutputFormat outputFormat = all.getGraphviz().getOutputFormat();

		// Render in image format
		all.getGraphviz().setOutputFormat(imageFormat);
		final File imageRawFile = this.createRenderedFile(all);

		// Render image map fragment
		all.getGraphviz().setOutputFormat(OutputFormat._Imagemap);
		final File imagemapRawFile = this.createRenderedFile(all);

		all.getGraphviz().setOutputFormat(outputFormat);

		try {
			// This has to be embedded in another file
			final File outputFile = File.createTempFile("temp-RenderedFile-", '.' + all.getGraphviz().getOutputFormat()
					.getFileExtension(), this.outputDir);

			try (FileOutputStream outputStream = new FileOutputStream(outputFile, false);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream,
							StandardCharsets.UTF_8));) {

				writer.write("<!DOCTYPE html>\n");
				writer.write("<html><head><title></title></head><body>");

				// Reader for the image map (<map> element) fragment
				try (InputStreamReader imagemapReader = new InputStreamReader(new FileInputStream(imagemapRawFile),
						StandardCharsets.UTF_8)) {
					// Include image map in HTML
					IOUtils.copy(imagemapReader, writer);
				}

				writer.write("<img src=\"data:");
				writer.write(imageFormat.getMimeType());
				writer.write(";base64,");

				// Input stream for the image file
				try (FileInputStream imageInputStream = new FileInputStream(imageRawFile)) {

					// Base64 encoder for the data: URI, writing to the output file

					// Flush the wirter to make sure we can write to outputStream directly
					writer.flush();
					Base64OutputStream base64Stream = new Base64OutputStream(outputStream, true, -1, null);
					IOUtils.copy(imageInputStream, base64Stream);
					// Flush the base64 stream to make sure we can use the writer again. We cannot close it because that
					// would close the outputStream too.
					base64Stream.flush();

				}

				writer.write("\" usemap=\"#cluster_vcat\"/>");

				writer.write("</body></html>");

			} catch (IOException e) {
				outputFile.delete();
				throw new VCatException("Failed to create output file", e);
			}

			return outputFile;

		} catch (IOException e) {
			throw new VCatException("Failed to create output file", e);
		}

	}

	@Override
	protected File createRenderedFileFromGraphFile(final AbstractAllParams<W> all, final File graphFile)
			throws VCatException {
		try {
			final File outputFile = File.createTempFile("temp-RenderedFile-", '.' + all.getGraphviz().getOutputFormat()
					.getFileExtension(), this.outputDir);
			graphviz.render(graphFile, outputFile, all.getCombined().getGraphviz());
			return outputFile;
		} catch (GraphvizException | IOException e) {
			throw new VCatException(e);
		}

	}

}
