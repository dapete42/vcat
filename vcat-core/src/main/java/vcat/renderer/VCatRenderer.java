package vcat.renderer;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vcat.AbstractVCat;
import vcat.VCatException;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.params.OutputFormat;
import vcat.params.VCatFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class VCatRenderer<W extends IWiki> extends AbstractVCatRenderer<W> {

    private static final long serialVersionUID = 7255644185587547207L;

    private static final Logger LOGGER = LoggerFactory.getLogger(VCatRenderer.class);

    private final Graphviz graphviz;

    private final Path outputDir;

    private final VCatFactory<W> vCatFactory;

    public VCatRenderer(final Graphviz graphviz, final Path tempDir, final ICategoryProvider<W> categoryProvider) {
        this.graphviz = graphviz;
        this.outputDir = tempDir;
        this.vCatFactory = new VCatFactory<>(categoryProvider);
    }

    @Override
    protected Path createGraphFile(final AbstractAllParams<W> all) throws VCatException {
        final AbstractVCat<W> vCat = this.vCatFactory.createInstance(all);
        try {
            final Path outputFile = Files.createTempFile(this.outputDir, "temp-Graph-", ".gv");
            vCat.renderToFile(outputFile);
            return outputFile;
        } catch (GraphvizException | IOException e) {
            throw new VCatException(e);
        }
    }

    @Override
    protected Path createImagemapHtmlFile(final AbstractAllParams<W> all, final OutputFormat imageFormat)
            throws VCatException {

        final OutputFormat outputFormat = all.getGraphviz().getOutputFormat();

        // Render in image format
        all.getGraphviz().setOutputFormat(imageFormat);
        final Path imageRawFile = this.createRenderedFile(all);

        // Render image map fragment
        all.getGraphviz().setOutputFormat(OutputFormat._Imagemap);
        final Path imagemapRawFile = this.createRenderedFile(all);

        all.getGraphviz().setOutputFormat(outputFormat);

        try {
            // This has to be embedded in another file
            final Path outputFile = Files.createTempFile(this.outputDir, "temp-RenderedFile-",
                    '.' + all.getGraphviz().getOutputFormat().getFileExtension());

            createImagemapHtmlFileComposeHtml(imageFormat, imageRawFile, imagemapRawFile, outputFile);

            return outputFile;

        } catch (IOException e) {
            throw new VCatException("Failed to create output file", e);
        }

    }

    private static void createImagemapHtmlFileComposeHtml(final OutputFormat imageFormat, final Path imageRawFile,
                                                          final Path imagemapRawFile, final Path outputFile) throws VCatException {
        try (OutputStream outputStream = Files.newOutputStream(outputFile, StandardOpenOption.CREATE);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(outputStreamWriter)) {

            writer.write("<!DOCTYPE html>\n");
            writer.write("<html><head><title></title></head><body>");

            // Reader for the image map (<map> element) fragment
            try (InputStream inputStream = Files.newInputStream(imagemapRawFile);
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // Include image map in HTML
                inputStreamReader.transferTo(writer);
            }

            writer.write("<img src=\"data:");
            writer.write(imageFormat.getMimeType());
            writer.write(";base64,");

            // Input stream for the image file
            try (InputStream imageInputStream = Files.newInputStream(imageRawFile)) {

                // Base64 encoder for the data: URI, writing to the output file

                // Flush the wirter to make sure we can write to outputStream directly
                writer.flush();
                Base64OutputStream base64Stream = new Base64OutputStream(outputStream, true, -1, null);
                imageInputStream.transferTo(base64Stream);
                // Flush the base64 stream to make sure we can use the writer again. We cannot close it because that
                // would close the outputStream too.
                base64Stream.flush();

            }

            writer.write("\" usemap=\"#cluster_vcat\"/>");

            writer.write("</body></html>");

        } catch (IOException e) {
            try {
                Files.delete(outputFile);
            } catch (IOException ee) {
                LOGGER.error("Failed to delete output file after error", ee);
            }
            throw new VCatException("Failed to create output file", e);
        }
    }

    @Override
    protected Path createRenderedFileFromGraphFile(final AbstractAllParams<W> all, final Path graphFile)
            throws VCatException {
        try {
            final Path outputFile = Files.createTempFile(this.outputDir, "temp-RenderedFile-",
                    '.' + all.getGraphviz().getOutputFormat().getFileExtension());
            graphviz.render(graphFile, outputFile, all.getCombined().getGraphviz());
            return outputFile;
        } catch (GraphvizException | IOException e) {
            throw new VCatException(e);
        }

    }

}
