package vcat.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vcat.VCatException;
import vcat.params.AbstractAllParams;
import vcat.params.OutputFormat;
import vcat.renderer.AbstractVCatRenderer;

public class TestVCatRenderer extends AbstractVCatRenderer<TestWiki> {

	public static class ImagemapHtmlFileEntry {

		public AbstractAllParams<TestWiki> all;

		public OutputFormat imageFormat;

	}

	public static class RenderedFileFromGraphFileEntry {

		public AbstractAllParams<TestWiki> all;

		public File graphFile;

	}

	private final List<AbstractAllParams<TestWiki>> createdGraphFiles = new ArrayList<>();

	private final List<ImagemapHtmlFileEntry> createdImagemapHtmlFiles = new ArrayList<>();

	private final List<RenderedFileFromGraphFileEntry> renderedFileFromGraphFiles = new ArrayList<>();

	private long delay = 0L;

	@Override
	protected File createGraphFile(AbstractAllParams<TestWiki> all) throws VCatException {
		delay();
		createdGraphFiles.add(all);
		return null;
	}

	@Override
	protected File createImagemapHtmlFile(AbstractAllParams<TestWiki> all, OutputFormat imageFormat)
			throws VCatException {
		delay();
		ImagemapHtmlFileEntry entry = new ImagemapHtmlFileEntry();
		entry.all = all;
		entry.imageFormat = imageFormat;
		createdImagemapHtmlFiles.add(entry);
		return null;
	}

	@Override
	protected File createRenderedFileFromGraphFile(AbstractAllParams<TestWiki> all, File graphFile)
			throws VCatException {
		delay();
		RenderedFileFromGraphFileEntry entry = new RenderedFileFromGraphFileEntry();
		entry.all = all;
		entry.graphFile = graphFile;
		renderedFileFromGraphFiles.add(entry);
		return null;
	}

	private void delay() {
		if (delay > 0L) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public List<AbstractAllParams<TestWiki>> getCreatedGraphFiles() {
		return createdGraphFiles;
	}

	public List<ImagemapHtmlFileEntry> getCreatedImagemapHtmlFiles() {
		return createdImagemapHtmlFiles;
	}

	public List<RenderedFileFromGraphFileEntry> getRenderedFileFromGraphFiles() {
		return renderedFileFromGraphFiles;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

}
