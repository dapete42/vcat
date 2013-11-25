package vcat;

import java.io.File;
import java.io.IOException;

import vcat.VCatException;
import vcat.cache.CacheException;
import vcat.cache.file.GraphFileCache;
import vcat.cache.file.RenderedFileCache;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.params.CombinedParams;
import vcat.params.OutputFormat;
import vcat.params.VCatFactory;

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
		final AbstractVCat<W> vCat = this.vCatFactory.createInstance(all);
		vCat.renderToCache(this.graphCache, tmpDir);
		File graphFile = this.graphCache.getCacheFile(all.getVCat());
		return graphFile;
	}

	private File createRenderedFile(AbstractAllParams<W> all, File tmpDir) throws CacheException, VCatException,
			GraphvizException {
		CombinedParams<W> combinedParams = all.getCombined();
		if (!this.renderedCache.containsKey(combinedParams)) {
			File graphFile = this.createGraphFile(all, tmpDir);
			// Parameters may have changed when creating the graph file, due to the handling of limit parameters, so we
			// have to check again
			if (!this.renderedCache.containsKey(combinedParams)) {
				File tmpFile;
				try {
					final String prefix = "RenderedFile-temp-";
					final String suffix = '.' + combinedParams.getGraphviz().getOutputFormat().getFileExtension();
					if (tmpDir == null) {
						tmpFile = File.createTempFile(prefix, suffix);
					} else {
						tmpFile = File.createTempFile(prefix, suffix, tmpDir);
					}
				} catch (IOException e) {
					throw new GraphvizException("Failed to create temporary file", e);
				}
				try {
					graphviz.render(graphFile, tmpFile, combinedParams.getGraphviz());
				} catch (GraphvizException e) {
					tmpFile.delete();
					throw e;
				}
				try {
					this.renderedCache.putFile(combinedParams, tmpFile, true);
				} catch (CacheException e) {
					tmpFile.delete();
					throw e;
				}

			}
		}
		return renderedCache.getCacheFile(combinedParams);
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
			if (all.getGraphviz().getOutputFormat() == OutputFormat.GraphvizRaw) {
				resultFile = createGraphFile(all, tmpDir);
			} else {
				resultFile = createRenderedFile(all, tmpDir);
			}

			return new RenderedFileInfo(resultFile, all.getGraphviz().getOutputFormat().getMimeType());
		} catch (CacheException | GraphvizException e) {
			throw new VCatException(e);
		}
	}

}
