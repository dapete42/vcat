package vcat.test;

import lombok.Getter;
import vcat.params.AbstractAllParams;
import vcat.params.OutputFormat;
import vcat.renderer.AbstractVCatRenderer;

import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestVCatRenderer extends AbstractVCatRenderer<TestWiki> {

    @Serial
    private static final long serialVersionUID = -7348028299089897262L;

    public static class ImagemapHtmlFileEntry {

        public OutputFormat imageFormat;

    }

    public static class RenderedFileFromGraphFileEntry {

        public Path graphFile;

    }

    @Getter
    private final List<AbstractAllParams<TestWiki>> createdGraphFiles = new ArrayList<>();

    @Getter
    private final List<ImagemapHtmlFileEntry> createdImagemapHtmlFiles = new ArrayList<>();

    @Getter
    private final List<RenderedFileFromGraphFileEntry> renderedFileFromGraphFiles = new ArrayList<>();

    private long delay = 0L;

    @Override
    protected Path createGraphFile(AbstractAllParams<TestWiki> all) {
        delay();
        createdGraphFiles.add(all);
        return null;
    }

    @Override
    protected Path createImagemapHtmlFile(AbstractAllParams<TestWiki> all, OutputFormat imageFormat) {
        delay();
        ImagemapHtmlFileEntry entry = new ImagemapHtmlFileEntry();
        entry.imageFormat = imageFormat;
        createdImagemapHtmlFiles.add(entry);
        return null;
    }

    @Override
    protected Path createRenderedFileFromGraphFile(AbstractAllParams<TestWiki> all, Path graphFile) {
        delay();
        RenderedFileFromGraphFileEntry entry = new RenderedFileFromGraphFileEntry();
        entry.graphFile = graphFile;
        renderedFileFromGraphFiles.add(entry);
        return null;
    }

    private void delay() {
        if (delay > 0L) {
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

}
