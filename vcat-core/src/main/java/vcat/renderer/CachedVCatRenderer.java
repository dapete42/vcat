package vcat.renderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import vcat.VCatException;
import vcat.cache.CacheException;
import vcat.cache.file.GraphFileCache;
import vcat.cache.file.RenderedFileCache;
import vcat.graphviz.Graphviz;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.params.CombinedParams;
import vcat.params.OutputFormat;
import vcat.params.VCatParams;

public class CachedVCatRenderer<W extends IWiki> extends VCatRenderer<W> {

	private static final long serialVersionUID = -5780692757065984451L;

	private final GraphFileCache<W> graphCache;

	private final int purge;

	private final RenderedFileCache<W> renderedCache;

	public CachedVCatRenderer(final Graphviz graphviz, final Path tempDir, final ICategoryProvider<W> categoryProvider,
			final Path cacheDir) throws VCatException {
		this(graphviz, tempDir, categoryProvider, cacheDir, 600);
	}

	public CachedVCatRenderer(final Graphviz graphviz, final Path tempDir, final ICategoryProvider<W> categoryProvider,
			final Path cacheDir, final int purge) throws VCatException {
		super(graphviz, tempDir, categoryProvider);
		this.purge = purge;

		Path graphCacheDir = cacheDir.resolve("graphFile");
		Path renderedFileCacheDir = cacheDir.resolve("renderedFile");

		try {
			Files.createDirectories(graphCacheDir);
			this.graphCache = new GraphFileCache<>(graphCacheDir, this.purge);
			Files.createDirectories(renderedFileCacheDir);
			this.renderedCache = new RenderedFileCache<>(renderedFileCacheDir, this.purge);
		} catch (CacheException | IOException e) {
			throw new VCatException("Error while setting up caches", e);
		}

		this.purge();
	}

	@Override
	protected Path createGraphFile(final AbstractAllParams<W> all) throws VCatException {
		final VCatParams<W> vCatParams = all.getVCat();
		if (!this.graphCache.containsKey(vCatParams)) {
			final Path otherFile = super.createGraphFile(all);
			try {
				this.graphCache.putFile(vCatParams, otherFile, true);
			} catch (CacheException e) {
				throw new VCatException(e);
			}
		}
		return this.graphCache.getCacheFile(vCatParams);
	}

	@Override
	protected Path createImagemapHtmlFile(final AbstractAllParams<W> all, final OutputFormat imageFormat)
			throws VCatException {
		final CombinedParams<W> combinedParams = all.getCombined();
		if (!this.renderedCache.containsKey(combinedParams)) {
			final Path otherFile = super.createImagemapHtmlFile(all, imageFormat);
			try {
				this.renderedCache.putFile(combinedParams, otherFile, true);
			} catch (CacheException e) {
				throw new VCatException(e);
			}
		}
		return this.renderedCache.getCacheFile(combinedParams);
	}

	@Override
	protected Path createRenderedFileFromGraphFile(final AbstractAllParams<W> all, final Path graphFile)
			throws VCatException {
		final CombinedParams<W> combinedParams = all.getCombined();
		if (!this.renderedCache.containsKey(combinedParams)) {
			final Path otherFile = super.createRenderedFileFromGraphFile(all, graphFile);
			try {
				this.renderedCache.putFile(combinedParams, otherFile, true);
			} catch (CacheException e) {
				throw new VCatException(e);
			}
		}
		return this.renderedCache.getCacheFile(combinedParams);
	}

	private void purge() throws VCatException {
		VCatException e = null;
		try {
			this.graphCache.purge();
		} catch (CacheException ee) {
			e = new VCatException("Error purging caches", ee);
		}
		try {
			this.renderedCache.purge();
		} catch (CacheException ee) {
			if (e == null) {
				e = new VCatException("Error purging caches", ee);
			} else {
				e.addSuppressed(ee);
			}
		}
		if (e != null) {
			throw e;
		}
	}

	@Override
	public RenderedFileInfo render(AbstractAllParams<W> all) throws VCatException {
		// Purge caches
		this.purge();
		return super.render(all);
	}

}
