package org.toolforge.vcat.renderer;

import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.cache.file.GraphFileCache;
import org.toolforge.vcat.cache.file.RenderedFileCache;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.mediawiki.interfaces.CategoryProvider;
import org.toolforge.vcat.params.AbstractAllParams;
import org.toolforge.vcat.params.CombinedParams;
import org.toolforge.vcat.params.OutputFormat;
import org.toolforge.vcat.params.VCatParams;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class CachedVCatRenderer extends VCatRenderer {

    @Serial
    private static final long serialVersionUID = -7813270634239995061L;

    private final GraphFileCache graphCache;

    private final int purge;

    private final RenderedFileCache renderedCache;

    public CachedVCatRenderer(final Graphviz graphviz, final Path tempDir, final CategoryProvider categoryProvider,
                              final Path cacheDir) throws VCatException {
        this(graphviz, tempDir, categoryProvider, cacheDir, 600);
    }

    public CachedVCatRenderer(final Graphviz graphviz, final Path tempDir, final CategoryProvider categoryProvider,
                              final Path cacheDir, final int purge) throws VCatException {
        super(graphviz, tempDir, categoryProvider);
        this.purge = purge;

        Path graphCacheDir = cacheDir.resolve("graphFile");
        Path renderedFileCacheDir = cacheDir.resolve("renderedFile");

        try {
            Files.createDirectories(graphCacheDir);
            this.graphCache = new GraphFileCache(graphCacheDir, this.purge);
            Files.createDirectories(renderedFileCacheDir);
            this.renderedCache = new RenderedFileCache(renderedFileCacheDir, this.purge);
        } catch (CacheException | IOException e) {
            throw new VCatException("Error while setting up caches", e);
        }

        this.purge();
    }

    @Override
    protected Path createGraphFile(final AbstractAllParams all) throws VCatException {
        final VCatParams vCatParams = all.getVCat();
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
    protected Path createImagemapHtmlFile(final AbstractAllParams all, final OutputFormat imageFormat)
            throws VCatException {
        final CombinedParams combinedParams = all.getCombined();
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
    protected Path createRenderedFileFromGraphFile(final AbstractAllParams all, final Path graphFile)
            throws VCatException {
        final CombinedParams combinedParams = all.getCombined();
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
        try (var pathStream = Files.list(outputDir)) {
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
                LOG.warn(Messages.getString("CachedVCatRenderer.Warn.CouldNotDeletePurging"), path, e);
            }
        }
        if (purgedFiles > 0) {
            LOG.info(Messages.getString("CachedVCatRenderer.Info.Purged"), purgedFiles);
        }
    }

    @Override
    public RenderedFileInfo render(AbstractAllParams all) throws VCatException {
        // Purge caches
        this.purge();
        return super.render(all);
    }

}
