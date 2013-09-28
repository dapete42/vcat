package vcat;

import java.io.File;
import java.io.IOException;

import vcat.VCatException;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;
import vcat.cache.IMetadataCache;
import vcat.cache.file.GraphFileCache;
import vcat.cache.file.RenderedFileCache;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.params.AllParams;
import vcat.params.CombinedParams;
import vcat.params.OutputFormat;

public class VCatRenderer {

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

	private final IApiCache apiCache;

	private final GraphFileCache graphCache;

	private final IMetadataCache metadataCache;

	private final Graphviz graphviz;

	private final int purge;

	private final RenderedFileCache renderedCache;

	private static void mkdirsWithError(File dir) throws VCatException {
		if (!dir.exists()) {
			final boolean created = dir.mkdirs();
			if (!created) {
				throw new VCatException("Could not create directory " + dir.getAbsolutePath());
			}
		}
	}

	public VCatRenderer(final Graphviz graphviz, final File tmpDir, final IApiCache apiCache,
			final IMetadataCache metadataCache) throws VCatException {
		this(graphviz, tmpDir, apiCache, metadataCache, 600);
	}

	public VCatRenderer(final Graphviz graphviz, final File tmpDir, final IApiCache apiCache,
			final IMetadataCache metadataCache, final int purge) throws VCatException {
		this.graphviz = graphviz;
		this.purge = purge;

		File graphCacheDir = new File(tmpDir, "graphFile");
		File renderedFileCacheDir = new File(tmpDir, "renderedFile");

		mkdirsWithError(graphCacheDir);
		mkdirsWithError(renderedFileCacheDir);

		try {
			this.apiCache = apiCache;
			this.metadataCache = metadataCache;
			this.graphCache = new GraphFileCache(graphCacheDir, this.purge);
			this.renderedCache = new RenderedFileCache(renderedFileCacheDir, this.purge);
		} catch (CacheException e) {
			throw new VCatException("Error while setting up caches", e);
		}

		this.purge();
	}

	private File createGraphFile(AllParams all) throws CacheException, VCatException, GraphvizException {
		final VCat vCat = VCat.createInstance(all);
		vCat.renderToCache(this.graphCache);
		File graphFile = this.graphCache.getCacheFile(all.getVCat());
		return graphFile;
	}

	private File createRenderedFile(AllParams all) throws CacheException, VCatException, GraphvizException {
		CombinedParams combinedParams = all.getCombined();
		if (!this.renderedCache.containsKey(combinedParams)) {
			File graphFile = this.createGraphFile(all);
			// Parameters may have changed when creating the graph file, due to the handling of limit parameters, so we
			// have to check again
			if (!this.renderedCache.containsKey(combinedParams)) {
				File tmpFile;
				try {
					tmpFile = File.createTempFile("RenderedFile-temp-", '.' + combinedParams.getGraphviz()
							.getOutputFormat().getFileExtension());
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
		this.apiCache.purge();
		this.graphCache.purge();
		this.renderedCache.purge();
		this.metadataCache.purge();
	}

	public RenderedFileInfo render(AllParams all) throws VCatException {
		try {
			// Purge caches
			this.purge();

			// Get and, if necessary, create result file
			final File resultFile;
			if (all.getGraphviz().getOutputFormat() == OutputFormat.GraphvizRaw) {
				resultFile = createGraphFile(all);
			} else {
				resultFile = createRenderedFile(all);
			}

			return new RenderedFileInfo(resultFile, all.getGraphviz().getOutputFormat().getMimeType());
		} catch (CacheException | GraphvizException e) {
			throw new VCatException(e);
		}
	}

}
