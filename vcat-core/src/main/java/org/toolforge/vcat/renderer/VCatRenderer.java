package org.toolforge.vcat.renderer;

import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.AbstractVCat;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.graphviz.GraphvizException;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.mediawiki.interfaces.CategoryProvider;
import org.toolforge.vcat.params.AbstractAllParams;
import org.toolforge.vcat.params.OutputFormat;
import org.toolforge.vcat.params.VCatFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

@Slf4j
public class VCatRenderer extends AbstractVCatRenderer {

    @Serial
    private static final long serialVersionUID = 7255644185587547207L;

    private final Graphviz graphviz;

    protected final Path outputDir;

    private final VCatFactory vCatFactory;

    public VCatRenderer(final Graphviz graphviz, final Path tempDir, final CategoryProvider categoryProvider) {
        this.graphviz = graphviz;
        outputDir = tempDir;
        vCatFactory = new VCatFactory(categoryProvider);
    }

    @Override
    protected Path createGraphFile(final AbstractAllParams all) throws VCatException {
        final AbstractVCat vCat = vCatFactory.createInstance(all);
        try {
            final Path outputFile = Files.createTempFile(outputDir, "temp-Graph-", ".gv");
            vCat.renderToFile(outputFile);
            return outputFile;
        } catch (GraphvizException | IOException e) {
            throw new VCatException(e);
        }
    }

    @Override
    protected Path createImagemapHtmlFile(final AbstractAllParams all, final OutputFormat imageFormat)
            throws VCatException {

        final OutputFormat outputFormat = all.getGraphviz().getOutputFormat();

        // Render in image format
        all.getGraphviz().setOutputFormat(imageFormat);
        final Path imageRawFile = createRenderedFile(all);

        // Render image map fragment
        all.getGraphviz().setOutputFormat(OutputFormat._Imagemap);
        final Path imagemapRawFile = createRenderedFile(all);

        all.getGraphviz().setOutputFormat(outputFormat);

        try {
            // This has to be embedded in another file
            final Path outputFile = Files.createTempFile(outputDir, "temp-RenderedFile-",
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

            // Read image file and output it as base64
            final var imageBytes = Files.readAllBytes(imageRawFile);
            final var base64String = Base64.getEncoder().encodeToString(imageBytes);
            writer.write(base64String);

            writer.write("\" usemap=\"#cluster_vcat\"/>");

            writer.write("</body></html>");

        } catch (IOException e) {
            try {
                Files.delete(outputFile);
            } catch (IOException ee) {
                LOG.error("Failed to delete output file after error", ee);
            }
            throw new VCatException("Failed to create output file", e);
        }
    }

    @Override
    protected Path createRenderedFileFromGraphFile(final AbstractAllParams all, final Path graphFile)
            throws VCatException {
        try {
            final Path outputFile = Files.createTempFile(outputDir, "temp-RenderedFile-",
                    '.' + all.getGraphviz().getOutputFormat().getFileExtension());
            graphviz.render(graphFile, outputFile, all.getCombined().getGraphviz());
            return outputFile;
        } catch (GraphvizException | IOException e) {
            throw new VCatException(e);
        }

    }

}
