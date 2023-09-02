package vcat.renderer;

import lombok.extern.slf4j.Slf4j;
import vcat.Messages;
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

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class CachedVCatRenderer<W extends IWiki> extends VCatRenderer<W> {

    @Serial
    private static final long serialVersionUID = -7813270634239995061L;

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
        try {
            this.purgeOutputDir();
        } catch (CacheException ee) {
            if (e == null) {
                e = new VCatException("Error purging output directory", ee);
            } else {
                e.addSuppressed(ee);
            }
        }
        if (e != null) {
            throw e;
        }
    }

    private void purgeOutputDir() throws CacheException {
        final long lastModifiedThreshold = System.currentTimeMillis() - (1000L * purge);
        final List<Path> filesToPurge;
        try (var pathStream = Files.walk(outputDir)) {
            filesToPurge = pathStream
                    .filter(path -> path.getFileName().toString().endsWith(".gv"))
                    .filter(path -> path.toFile().lastModified() < lastModifiedThreshold)
                    .toList();
        } catch (IOException e) {
            throw new CacheException(e);
        }
        int purgedFiles = 0;
        for (var path : filesToPurge) {
            try {
                Files.delete(path);
                purgedFiles++;
            } catch (IOException e) {
                log.warn(Messages.getString("CachedVCatRenderer.Warn.CouldNotDeletePurging"), path, e);
            }
        }
        if (purgedFiles > 0) {
            log.info(Messages.getString("CachedVCatRenderer.Info.Purged"), purgedFiles);
        }
    }

    @Override
    public RenderedFileInfo render(AbstractAllParams<W> all) throws VCatException {
        // Purge caches
        this.purge();
        return super.render(all);
    }

}
