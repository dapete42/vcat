package vcat;

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

import vcat.VCatException;
import vcat.cache.CacheException;
import vcat.cache.file.GraphFileCache;
import vcat.cache.file.RenderedFileCache;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.params.Link;
import vcat.params.OutputFormat;
import vcat.params.VCatFactory;
import vcat.params.VCatParams;

public class VCatRenderer<W extends IWiki> {

	public class RenderedFileInfo {

		private final File file;

		private final String mimeType;

		public RenderedFileInfo(File file, String mimeType) {
			this.file = file;
			this.mimeType = mimeType;
		}

		public File getFile() {
			return file;
		}

		public String getMimeType() {
			return mimeType;
		}

	}

	private final GraphFileCache<W> graphCache;

	private final Graphviz graphviz;

	private final int purge;

	private final RenderedFileCache<W> renderedCache;

	private final VCatFactory<W> vCatFactory;

	private static void mkdirsWithError(File dir) throws VCatException {
		if (!dir.exists()) {
			final boolean created = dir.mkdirs();
			if (!created) {
				throw new VCatException("Could not create directory " + dir.getAbsolutePath());
			}
		}
	}

	public VCatRenderer(final Graphviz graphviz, final File cacheDir, final ICategoryProvider<W> categoryProvider)
			throws VCatException {
		this(graphviz, cacheDir, categoryProvider, 600);
	}

	public VCatRenderer(final Graphviz graphviz, final File cacheDir, final ICategoryProvider<W> categoryProvider,
			final int purge) throws VCatException {
		this.graphviz = graphviz;
		this.purge = purge;

		File graphCacheDir = new File(cacheDir, "graphFile");
		File renderedFileCacheDir = new File(cacheDir, "renderedFile");

		mkdirsWithError(graphCacheDir);
		mkdirsWithError(renderedFileCacheDir);

		try {
			this.graphCache = new GraphFileCache<W>(graphCacheDir, this.purge);
			this.renderedCache = new RenderedFileCache<W>(renderedFileCacheDir, this.purge);
			this.vCatFactory = new VCatFactory<W>(categoryProvider);
		} catch (CacheException e) {
			throw new VCatException("Error while setting up caches", e);
		}

		this.purge();
	}

	private File createGraphFile(AbstractAllParams<W> all, File tmpDir) throws CacheException, VCatException,
			GraphvizException {
		final VCatParams<W> vCatParams = all.getVCat();
		if (!this.graphCache.containsKey(vCatParams)) {
			final AbstractVCat<W> vCat = this.vCatFactory.createInstance(all);
			vCat.renderToCache(this.graphCache, tmpDir);
		}
		File graphFile = this.graphCache.getCacheFile(vCatParams);
		return graphFile;
	}

	private File createImagemapHtmlFile(AbstractAllParams<W> all, File tmpDir) throws CacheException, VCatException,
			GraphvizException {

		// Change to alternative output format for further processing
		final OutputFormat originalOutputFormat = all.getGraphviz().getOutputFormat();
		final OutputFormat newOutputFormat = originalOutputFormat.getImageMapOutputFormat();
		all.getGraphviz().setOutputFormat(newOutputFormat);

		if (!this.renderedCache.containsKey(all.getCombined())) {

			// Render in original output format
			all.getGraphviz().setOutputFormat(originalOutputFormat);
			final File imageRawFile = this.createRenderedFile(all, tmpDir);

			// Render as image map
			all.getGraphviz().setOutputFormat(OutputFormat._Imagemap);
			final File imagemapRawFile = this.createRenderedFile(all, tmpDir);

			// Restore new output format
			all.getGraphviz().setOutputFormat(newOutputFormat);

			// This has to be embedded in another file
			File tmpFile;
			try {
				final String prefix = "RenderedFile-temp-";
				final String suffix = '.' + all.getGraphviz().getOutputFormat().getFileExtension();
				if (tmpDir == null) {
					tmpFile = File.createTempFile(prefix, suffix);
				} else {
					tmpFile = File.createTempFile(prefix, suffix, tmpDir);
				}
			} catch (IOException e) {
				throw new GraphvizException("Failed to create temporary file", e);
			}

			try (FileOutputStream outputStream = new FileOutputStream(tmpFile, false);
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
				writer.write(originalOutputFormat.getMimeType());
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
				tmpFile.delete();
				throw new VCatException("Failed to create temporary file", e);
			}
			try {
				this.renderedCache.putFile(all.getCombined(), tmpFile, true);
			} catch (CacheException e) {
				tmpFile.delete();
				throw e;
			}
		}
		return renderedCache.getCacheFile(all.getCombined());
	}

	private File createRenderedFile(AbstractAllParams<W> all, File tmpDir) throws CacheException, VCatException,
			GraphvizException {
		if (!this.renderedCache.containsKey(all.getCombined())) {
			final File graphFile = this.createGraphFile(all, tmpDir);
			// Parameters may have changed when creating the graph file, due to the handling of limit parameters, so we
			// have to check again
			if (!this.renderedCache.containsKey(all.getCombined())) {
				File tmpFile;
				try {
					final String prefix = "RenderedFile-temp-";
					final String suffix = '.' + all.getCombined().getGraphviz().getOutputFormat().getFileExtension();
					if (tmpDir == null) {
						tmpFile = File.createTempFile(prefix, suffix);
					} else {
						tmpFile = File.createTempFile(prefix, suffix, tmpDir);
					}
				} catch (IOException e) {
					throw new GraphvizException("Failed to create temporary file", e);
				}
				try {
					graphviz.render(graphFile, tmpFile, all.getCombined().getGraphviz());
				} catch (GraphvizException e) {
					tmpFile.delete();
					throw e;
				}
				try {
					this.renderedCache.putFile(all.getCombined(), tmpFile, true);
				} catch (CacheException e) {
					tmpFile.delete();
					throw e;
				}

			}
		}
		return renderedCache.getCacheFile(all.getCombined());
	}

	public void purge() {
		this.graphCache.purge();
		this.renderedCache.purge();
	}

	public RenderedFileInfo render(AbstractAllParams<W> all) throws VCatException {
		return this.render(all, null);
	}

	public RenderedFileInfo render(AbstractAllParams<W> all, File tmpDir) throws VCatException {
		try {
			// Purge caches
			this.purge();

			// Get and, if necessary, create result file
			final File resultFile;
			final OutputFormat outputFormat = all.getGraphviz().getOutputFormat();
			if (outputFormat == OutputFormat.GraphvizRaw) {
				// GraphvizRaw returns just the graph file
				resultFile = createGraphFile(all, tmpDir);
			} else if (all.getVCat().getLink() != Link.None && outputFormat.hasImageMapOutputFormat()) {
				// If links are requested, some formats want to be shown in an HTML page with an image map
				resultFile = createImagemapHtmlFile(all, tmpDir);
			} else {
				// Everything else returns the rendered file
				resultFile = createRenderedFile(all, tmpDir);
			}

			return new RenderedFileInfo(resultFile, all.getGraphviz().getOutputFormat().getMimeType());
		} catch (CacheException | GraphvizException e) {
			throw new VCatException(e);
		}
	}

}
