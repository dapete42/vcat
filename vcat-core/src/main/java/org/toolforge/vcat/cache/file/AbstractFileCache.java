package org.toolforge.vcat.cache.file;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.util.HashHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract base class for caches to store generic {@link Serializable} objects, backed by files in a directory in the
 * file system.
 *
 * @param <K> Class used for key.
 * @author Peter Schlömer
 */
@Slf4j
public abstract class AbstractFileCache<K extends Serializable> {

    /**
     * The cache directory.
     */
    @Getter
    protected final Path cacheDirectory;

    protected final Lock lock = new ReentrantLock();

    /**
     * Maximum age of cached items in seconds
     */
    protected int maxAgeInSeconds;

    /**
     * File name prefix.
     */
    protected final String prefix;

    /**
     * File name suffix.
     */
    protected final String suffix;

    /**
     * Internal constructor called by subclasses.
     *
     * @param cacheDirectory  The cache directory. This must exist and be writeable.
     * @param prefix          File name prefix.
     * @param suffix          File name suffix.
     * @param maxAgeInSeconds Maximum age of cached items in seconds.
     * @throws CacheException If the directory does not exist or is not writeable.
     */
    protected AbstractFileCache(Path cacheDirectory, String prefix, String suffix, final int maxAgeInSeconds)
            throws CacheException {
        if (!Files.exists(cacheDirectory) || !Files.isDirectory(cacheDirectory) || !Files.isWritable(cacheDirectory)) {
            throw new CacheException(MessageFormatter
                    .format(Messages.getString("AbstractFileCache.Exception.DirMustExist"), cacheDirectory)
                    .getMessage());
        }
        this.cacheDirectory = cacheDirectory;
        this.prefix = prefix;
        this.suffix = suffix;
        this.maxAgeInSeconds = maxAgeInSeconds;
    }

    public void clear() throws CacheException {
        lock.lock();
        try {
            int clearedFiles = 0;
            for (Path path : this.getAllFiles()) {
                try {
                    Files.delete(path);
                    clearedFiles++;
                } catch (IOException e) {
                    LOG.warn(Messages.getString("AbstractFileCache.Warn.CouldNotDeleteClearing"), path, e);
                }
            }
            if (clearedFiles > 0) {
                LOG.info(Messages.getString("AbstractFileCache.Info.Cleared"), clearedFiles);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(K key) {
        lock.lock();
        try {
            return Files.exists(this.getCacheFile(key));
        } finally {
            lock.unlock();
        }
    }

    public byte[] get(K key) throws CacheException {
        lock.lock();
        try {
            Path cacheFile = this.getCacheFile(key);
            if (Files.exists(cacheFile)) {
                try {
                    return Files.readAllBytes(cacheFile);
                } catch (IOException e) {
                    throw new CacheException(e);
                }
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return List of all files used by the cache.
     */
    private List<Path> getAllFiles() throws CacheException {
        lock.lock();
        try {
            try (Stream<Path> pathStream = Files.list(cacheDirectory)) {
                return pathStream.filter(s -> s.getFileName().toString().startsWith(this.prefix))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new CacheException("Error reading list of files", e);
            }
        } finally {
            lock.unlock();
        }
    }

    public InputStream getAsInputStream(K key) throws CacheException {
        lock.lock();
        try {
            if (this.containsKey(key)) {
                try {
                    return Files.newInputStream(this.getCacheFile(key));
                } catch (Exception e) {
                    throw new CacheException(Messages.getString("AbstractFileCache.Exception.IOReading"), e);
                }
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param key Key.
     * @return The file the item with the specified key is stored in.
     */
    public Path getCacheFile(K key) {
        return this.cacheDirectory.resolve(this.getCacheFilename(key));
    }

    /**
     * @param key Key.
     * @return The filename to use to store a specified key in.
     */
    protected String getCacheFilename(K key) {
        return prefix + this.hashForKey(key) + suffix;
    }

    /**
     * Return a hash string for a key. This must be implemented by subclasses.
     *
     * @param key Key.
     * @return A hash string for a key.
     */
    protected String hashForKey(K key) {
        return HashHelper.sha256Hex(key);
    }

    public void purge() throws CacheException {
        lock.lock();
        try {
            if (maxAgeInSeconds < 0) {
                return;
            }
            long lastModifiedThreshold = System.currentTimeMillis() - (1000L * this.maxAgeInSeconds);
            int purgedFiles = 0;
            for (Path path : this.getAllFiles()) {
                if (path.toFile().lastModified() < lastModifiedThreshold) {
                    try {
                        Files.delete(path);
                        purgedFiles++;
                    } catch (IOException e) {
                        LOG.warn(Messages.getString("AbstractFileCache.Warn.CouldNotDeletePurging"), path, e);
                    }
                }
            }
            if (purgedFiles > 0) {
                LOG.info(Messages.getString("AbstractFileCache.Info.Purged"), purgedFiles);
            }
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, byte[] value) throws CacheException {
        lock.lock();
        try {
            try (OutputStream outputStream = Files.newOutputStream(this.getCacheFile(key))) {
                this.writeValueToStream(value, outputStream);
            } catch (IOException e) {
                throw new CacheException(Messages.getString("AbstractFileCache.Exception.WriteFailed"), e);
            }
        } finally {
            lock.unlock();
        }
    }

    public void putFile(K key, Path file, boolean move) throws CacheException {
        lock.lock();
        try {
            Path cacheFile = this.getCacheFile(key);
            // Delete file in cache, if it exists
            try {
                Files.deleteIfExists(cacheFile);
            } catch (IOException e) {
                throw new CacheException(Messages.getString("AbstractFileCache.Exception.DeleteFailed"), e);
            }
            if (move) {
                try {
                    Files.move(file, cacheFile);
                } catch (IOException e) {
                    throw new CacheException(Messages.getString("AbstractFileCache.Exception.MoveFailed"), e);
                }
            } else {
                try {
                    Files.copy(file, cacheFile);
                } catch (IOException e) {
                    throw new CacheException(Messages.getString("AbstractFileCache.Exception.MoveFailed"), e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void remove(K key) throws CacheException {
        lock.lock();
        try {
            try {
                Files.delete(this.getCacheFile(key));
            } catch (IOException e) {
                throw new CacheException(Messages.getString("AbstractFileCache.Exception.DeleteFailed"), e);
            }
        } finally {
            lock.unlock();
        }
    }

    protected void writeValueToStream(byte[] value, OutputStream outputStream) throws CacheException {
        try {
            outputStream.write(value);
        } catch (IOException e) {
            throw new CacheException(Messages.getString("AbstractFileCache.Exception.IOWriting"), e);
        }
    }

}
