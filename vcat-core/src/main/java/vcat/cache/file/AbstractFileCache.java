package vcat.cache.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.Messages;
import vcat.cache.CacheException;

/**
 * Abstract base class for caches to store generic {@link Serializable} objects, backed by files in a directory in the
 * file system.
 * 
 * @author Peter Schlömer
 * 
 * @param <K>
 *            Class used for key.
 */
public abstract class AbstractFileCache<K extends Serializable> {

	private final Log log = LogFactory.getLog(this.getClass());

	/** The cache directory. */
	protected final File cacheDirectory;

	/** Maximum age of cached items in seconds */
	protected int maxAgeInSeconds;

	/** File name prefix. */
	protected final String prefix;

	/** File name suffix. */
	protected final String suffix;

	/**
	 * Internal constructor called by subclasses.
	 * 
	 * @param cacheDirectory
	 *            The cache directory. This must exist and be writeable.
	 * @param prefix
	 *            File name prefix.
	 * @param suffix
	 *            File name suffix.
	 * @throws CacheException
	 *             If the directory does not exist or is not writeable.
	 */
	protected AbstractFileCache(File cacheDirectory, String prefix, String suffix, final int maxAgeInSeconds)
			throws CacheException {
		if (!cacheDirectory.exists() || !cacheDirectory.isDirectory() || !cacheDirectory.canWrite()) {
			throw new CacheException(String.format(Messages.getString("AbstractFileCache.Exception.DirMustExist"),
					cacheDirectory.getAbsolutePath()));
		}
		this.cacheDirectory = cacheDirectory;
		this.prefix = prefix;
		this.suffix = suffix;
		this.maxAgeInSeconds = maxAgeInSeconds;
	}

	public synchronized void clear() {
		int clearedFiles = 0;
		for (File file : this.getAllFiles()) {
			if (file.delete()) {
				clearedFiles++;
			} else {
				log.warn(String.format(Messages.getString("AbstractFileCache.Warn.CouldNotDeleteClearing"),
						file.getAbsolutePath()));
			}
		}
		if (clearedFiles > 0) {
			log.info(String.format(Messages.getString("AbstractFileCache.Info.Cleared"), clearedFiles));
		}
	}

	public synchronized boolean containsKey(K key) {
		return this.getCacheFile(key).exists();
	}

	public synchronized byte[] get(K key) throws CacheException {
		File cacheFile = this.getCacheFile(key);
		if (cacheFile.exists()) {
			FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(cacheFile);
			} catch (FileNotFoundException e) {
				throw new CacheException(String.format(Messages.getString("AbstractFileCache.Exception.FileNotFound"),
						cacheFile.getAbsolutePath()));
			}
			return this.readValueFromStream(inputStream);
		} else {
			return null;
		}
	}

	/**
	 * @return Array of all files used by the cache.
	 */
	private File[] getAllFiles() {
		return this.cacheDirectory.listFiles((FilenameFilter) FileFilterUtils.prefixFileFilter(this.prefix));
	}

	public synchronized InputStream getAsInputStream(K key) throws CacheException {
		if (this.containsKey(key)) {
			try {
				return new FileInputStream(this.getCacheFile(key));
			} catch (FileNotFoundException e) {
				String message = Messages.getString("AbstractFileCache.Exception.FileNotFoundShouldNotHappen");
				log.error(message, e);
				throw new CacheException(message, e);
			}
		} else {
			return null;
		}
	}

	/**
	 * @return The cache directory.
	 */
	public File getCacheDirectory() {
		return this.cacheDirectory;
	}

	/**
	 * @param key
	 *            Key.
	 * @return The file the item with the specified key is stored in.
	 */
	public File getCacheFile(K key) {
		return new File(this.cacheDirectory, this.getCacheFilename(key));
	}

	/**
	 * @param key
	 *            Key.
	 * @return The filename to use to store a specified key in.
	 */
	protected String getCacheFilename(K key) {
		return prefix + this.hashForKey(key) + suffix;
	}

	/**
	 * Create a hash value from an array of bytes with data. Currently uses SHA-1, converted to a hex string.
	 * 
	 * @param bytes
	 *            Data.
	 * @return A hash value from the data.
	 */
	protected String hash(byte[] bytes) {
		return DigestUtils.sha1Hex(bytes);
	}

	/**
	 * Return a hash string for a key. This must be implemented by subclasses.
	 * 
	 * @param key
	 *            Key.
	 * @return A hash string for a key.
	 */
	protected String hashForKey(K key) {
		return this.hash(SerializationUtils.serialize(key));
	}

	public synchronized void purge() {
		long lastModifiedThreshold = System.currentTimeMillis() - (1000l * this.maxAgeInSeconds);
		int purgedFiles = 0;
		for (File file : this.getAllFiles()) {
			if (file.lastModified() < lastModifiedThreshold) {
				if (file.delete()) {
					purgedFiles++;
				} else {
					log.warn(String.format(Messages.getString("AbstractFileCache.Warn.CouldNotDeletePurging"),
							file.getAbsolutePath()));
				}
			}
		}
		if (purgedFiles > 0) {
			log.info(String.format(Messages.getString("AbstractFileCache.Info.Purged"), purgedFiles));
		}
	}

	public synchronized void put(K key, byte[] value) throws CacheException {
		try {
			FileOutputStream outputStream = new FileOutputStream(this.getCacheFile(key));
			this.writeValueToStream(value, outputStream);
			outputStream.close();
		} catch (IOException e) {
			throw new CacheException(Messages.getString("AbstractFileCache.Exception.WriteFailed"), e);
		}
	}

	public synchronized void putFile(K key, File file, boolean move) throws CacheException {
		File cacheFile = this.getCacheFile(key);
		// Delete file in cache, if it exists
		cacheFile.delete();
		if (move) {
			// Try to use rename to move the tempoary file to the cache
			if (!file.renameTo(cacheFile)) {
				// If this fails, we need to first copy, then delete
				try {
					FileUtils.copyFile(file, cacheFile);
				} catch (IOException e) {
					throw new CacheException(Messages.getString("AbstractFileCache.Exception.MoveFailed"), e);
				} finally {
					file.delete();
				}
			}
		} else {
			try {
				FileUtils.copyFile(file, cacheFile);
			} catch (IOException e) {
				throw new CacheException(Messages.getString("AbstractFileCache.Exception.MoveFailed"), e);
			}
		}
	}

	protected synchronized byte[] readValueFromStream(InputStream inputStream) throws CacheException {
		try {
			return IOUtils.toByteArray(inputStream);
		} catch (IOException e) {
			throw new CacheException(Messages.getString("AbstractFileCache.Exception.IOReading"), e);
		}
	}

	public synchronized void remove(K key) {
		this.getCacheFile(key).delete();
	}

	protected synchronized void writeValueToStream(byte[] value, OutputStream outputStream) throws CacheException {
		try {
			IOUtils.write(value, outputStream);
		} catch (IOException e) {
			throw new CacheException(Messages.getString("AbstractFileCache.Exception.IOWriting"), e);
		}
	}

}
