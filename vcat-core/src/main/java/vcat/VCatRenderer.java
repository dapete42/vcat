package vcat;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import vcat.VCatException;
import vcat.cache.ApiCache;
import vcat.cache.CacheException;
import vcat.cache.GraphCache;
import vcat.cache.MetadataCache;
import vcat.cache.RenderedFileCache;
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

	private final ApiCache apiCache;

	private final GraphCache graphCache;

	private final MetadataCache metadataCache;

	private final Graphviz graphviz;

	private int purge = 600;

	private int purgeMetadata = 86400;

	private final RenderedFileCache renderedCache;

	private static void mkdirsWithError(File dir) throws VCatException {
		if (!dir.exists()) {
			final boolean created = dir.mkdirs();
			if (!created) {
				throw new VCatException("Could not create directory " + dir.getAbsolutePath());
			}
		}
	}

	public VCatRenderer(File tmpDir, Graphviz graphviz) throws VCatException {
		this.graphviz = graphviz;

		File apiCacheDir = new File(tmpDir, "api");
		File metadataCacheDir = new File(tmpDir, "metadata");
		File graphCacheDir = new File(tmpDir, "graphFile");
		File renderedFileCacheDir = new File(tmpDir, "renderedFile");

		mkdirsWithError(apiCacheDir);
		mkdirsWithError(metadataCacheDir);
		mkdirsWithError(graphCacheDir);
		mkdirsWithError(renderedFileCacheDir);

		try {
			this.apiCache = new ApiCache(apiCacheDir);
			this.metadataCache = new MetadataCache(metadataCacheDir);
			this.graphCache = new GraphCache(graphCacheDir);
			this.renderedCache = new RenderedFileCache(renderedFileCacheDir);
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

	public int getPurge() {
		return this.purge;
	}

	public int getPurgeMetadata() {
		return this.purgeMetadata;
	}

	public void purge() {
		this.apiCache.purge(this.purge);
		this.graphCache.purge(this.purge);
		this.renderedCache.purge(this.purge);
		this.metadataCache.purge(this.purgeMetadata);
	}

	public RenderedFileInfo render(Map<String, String[]> parameterMap) throws VCatException {
		try {
			// Purge caches
			this.purge();

			// Get parameters
			final AllParams all = new AllParams(parameterMap, this.metadataCache, this.apiCache);

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

	public void setPurge(int purge) {
		this.purge = purge;
	}

	public void setPurgeMetadata(int purgeMetadata) {
		this.purgeMetadata = purgeMetadata;
	}

}
