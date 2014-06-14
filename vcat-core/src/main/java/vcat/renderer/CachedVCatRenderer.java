package vcat.renderer;

import java.io.File;

import vcat.VCatException;
import vcat.cache.CacheException;
import vcat.cache.file.GraphFileCache;
import vcat.cache.file.RenderedFileCache;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.params.CombinedParams;
import vcat.params.OutputFormat;
import vcat.params.VCatParams;

public class CachedVCatRenderer<W extends IWiki> extends AbstractVCatRenderer<W> {

	private final GraphFileCache<W> graphCache;

	private final AbstractVCatRenderer<W> otherRenderer;

	private final int purge;

	private final RenderedFileCache<W> renderedCache;

	public CachedVCatRenderer(final AbstractVCatRenderer<W> otherRenderer, final File cacheDir) throws VCatException {
		this(otherRenderer, cacheDir, 600);
	}

	public CachedVCatRenderer(final AbstractVCatRenderer<W> otherRenderer, final File cacheDir, final int purge)
			throws VCatException {
		this.otherRenderer = otherRenderer;
		this.purge = purge;

		File graphCacheDir = new File(cacheDir, "graphFile");
		File renderedFileCacheDir = new File(cacheDir, "renderedFile");

		graphCacheDir.mkdirs();
		renderedFileCacheDir.mkdirs();

		try {
			this.graphCache = new GraphFileCache<>(graphCacheDir, this.purge);
			this.renderedCache = new RenderedFileCache<>(renderedFileCacheDir, this.purge);
		} catch (CacheException e) {
			throw new VCatException("Error while setting up caches", e);
		}

		this.purge();
	}

	@Override
	protected File createGraphFile(final AbstractAllParams<W> all) throws VCatException {
		final VCatParams<W> vCatParams = all.getVCat();
		if (!this.graphCache.containsKey(vCatParams)) {
			final File otherFile = otherRenderer.createGraphFile(all);
			try {
				this.graphCache.putFile(vCatParams, otherFile, true);
			} catch (CacheException e) {
				throw new VCatException(e);
			}
		}
		return this.graphCache.getCacheFile(vCatParams);
	}

	@Override
	protected File createImagemapHtmlFile(final AbstractAllParams<W> all, OutputFormat imageFormat)
			throws VCatException {
		final CombinedParams<W> combinedParams = all.getCombined();
		if (!this.renderedCache.containsKey(combinedParams)) {
			final File otherFile = otherRenderer.createImagemapHtmlFile(all, imageFormat);
			try {
				this.renderedCache.putFile(combinedParams, otherFile, true);
			} catch (CacheException e) {
				throw new VCatException(e);
			}
		}
		return this.renderedCache.getCacheFile(combinedParams);
	}

	@Override
	protected File createRenderedFileFromGraphFile(final AbstractAllParams<W> all, final File graphFile)
			throws VCatException {
		final CombinedParams<W> combinedParams = all.getCombined();
		if (!this.renderedCache.containsKey(combinedParams)) {
			final File otherFile = otherRenderer.createRenderedFileFromGraphFile(all, graphFile);
			try {
				this.renderedCache.putFile(combinedParams, otherFile, true);
			} catch (CacheException e) {
				throw new VCatException(e);
			}
		}
		return this.renderedCache.getCacheFile(combinedParams);
	}

	private void purge() {
		this.graphCache.purge();
		this.renderedCache.purge();
	}

	@Override
	public RenderedFileInfo render(AbstractAllParams<W> all) throws VCatException {
		// Purge caches
		this.purge();
		return super.render(all);
	}

}
