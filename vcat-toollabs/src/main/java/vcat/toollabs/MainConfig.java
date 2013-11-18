package vcat.toollabs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.VCatException;

public class MainConfig {

	private final Log log = LogFactory.getLog(this.getClass());

	/** Default value for {@link #graphvizDir} */
	private static final String DEFAULT_GRAPHVIZ_DIR = "/usr/bin";

	/** Default value for {@link #graphvizProcesses} */
	private static final int DEFAULT_GRAPHVIZ_PROCESSES = 0;

	/** Default value for {@link #purge} */
	private static final int DEFAULT_PURGE = 600;

	/** Default value for {@link #purgeMetadata} */
	private static final int DEFAULT_PURGE_METADATA = 86400;

	/** Default value for {@link #redisServerPort} */
	private static final int DEFAULT_REDIS_SERVER_PORT = 6379;

	public String cacheDir;

	public String graphvizDir;

	public int graphvizProcesses;

	public String jdbcUrl;

	public int purge;

	public int purgeMetadata;

	public String redisSecret;

	public String redisChannelControl;

	public String redisChannelRequest;

	public String redisChannelResponse;

	private String redisKeyRequestSuffix;

	private String redisKeyResponseErrorSuffix;

	private String redisKeyResponseHeadersSuffix;

	private String redisKeyResponseSuffix;

	public String redisServerHostname;

	public int redisServerPort;

	public String buildRedisKeyRequest(final String jedisKey) {
		return this.redisSecret + '-' + jedisKey + this.redisKeyRequestSuffix;
	}

	public String buildRedisKeyResponse(final String jedisKey) {
		return this.redisSecret + '-' + jedisKey + this.redisKeyResponseSuffix;
	}

	public String buildRedisKeyResponseError(final String jedisKey) {
		return this.redisSecret + '-' + jedisKey + this.redisKeyResponseErrorSuffix;
	}

	public String buildRedisKeyResponseHeaders(final String jedisKey) {
		return this.redisSecret + '-' + jedisKey + this.redisKeyResponseHeadersSuffix;
	}

	public boolean readFromPropertyFile(final File propertiesFile) throws VCatException {

		Properties properties = new Properties();
		try {
			BufferedReader propertiesReader = new BufferedReader(new InputStreamReader(new FileInputStream(
					propertiesFile), StandardCharsets.UTF_8));
			properties.load(propertiesReader);
		} catch (IOException e) {
			throw new VCatException(String.format(Messages.getString("MainConfig.Exception.ErrorReadingProperties"),
					propertiesFile.getAbsolutePath()), e);
		}

		int errors = 0;

		this.cacheDir = properties.getProperty("cache.dir");
		if (this.cacheDir == null || this.cacheDir.isEmpty()) {
			log.error("Property cache.dir missing or empty");
			errors++;
		}

		this.graphvizDir = properties.getProperty("graphviz.dir");
		if (this.graphvizDir == null || this.graphvizDir.isEmpty()) {
			this.graphvizDir = DEFAULT_GRAPHVIZ_DIR;
			log.info(String.format(Messages.getString("Info.PropertyDefault"), "graphviz.dir", this.graphvizDir));
		}

		final String graphvizProcessesString = properties.getProperty("graphviz.processes");
		if (graphvizProcessesString == null || graphvizProcessesString.isEmpty()) {
			this.graphvizProcesses = DEFAULT_GRAPHVIZ_PROCESSES;
			log.info(String.format(Messages.getString("Info.PropertyDefault"), "graphviz.processes",
					this.graphvizProcesses));
		} else {
			try {
				this.graphvizProcesses = Integer.parseInt(graphvizProcessesString);
			} catch (NumberFormatException e) {
				log.error(String.format(Messages.getString("Error.PropertyNotANumber"), "graphviz.processes"), e);
				errors++;
			}
		}

		this.jdbcUrl = properties.getProperty("jdbc.url");
		if (this.jdbcUrl == null || this.jdbcUrl.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"), "jdbc.url"));
			errors++;
		}

		final String purgeString = properties.getProperty("purge");
		if (purgeString == null || purgeString.isEmpty()) {
			this.purge = DEFAULT_PURGE;
			log.info(String.format(Messages.getString("Info.PropertyDefault"), "purge", this.purge));
		} else {
			try {
				this.purge = Integer.parseInt(purgeString);
			} catch (NumberFormatException e) {
				log.error(String.format(Messages.getString("Error.PropertyNotANumber"), "purge"), e);
				errors++;
			}
		}

		final String purgeMetadataString = properties.getProperty("purge.metadata");
		if (purgeMetadataString == null || purgeMetadataString.isEmpty()) {
			this.purgeMetadata = DEFAULT_PURGE_METADATA;
			log.info(String.format(Messages.getString("Info.PropertyDefault"), "purge.metadata", this.purgeMetadata));
		} else {
			try {
				this.purgeMetadata = Integer.parseInt(purgeMetadataString);
			} catch (NumberFormatException e) {
				log.error(String.format(Messages.getString("Error.PropertyNotANumber"), "purge.metadata"), e);
				errors++;
			}
		}

		this.redisServerHostname = properties.getProperty("redis.server.hostname");
		if (this.redisServerHostname == null || this.redisServerHostname.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"), "redis.server.hostname"));
			errors++;
		}

		final String redisServerPortString = properties.getProperty("redis.server.port");
		if (redisServerPortString == null || redisServerPortString.isEmpty()) {
			this.redisServerPort = DEFAULT_REDIS_SERVER_PORT;
			log.info(String.format(Messages.getString("Info.PropertyDefault"), "redis.server.port",
					this.redisServerPort));
		} else {
			try {
				this.redisServerPort = Integer.parseInt(redisServerPortString);
			} catch (NumberFormatException e) {
				log.error(String.format(Messages.getString("Error.PropertyNotANumber"), "redis.server.port"), e);
				errors++;
			}
		}

		this.redisSecret = properties.getProperty("redis.secret");
		if (this.redisSecret == null) {
			log.error(String.format(Messages.getString("Error.PropertyMissing"), "redis.secret"));
			errors++;
		} else {
			log.info(String.format(Messages.getString("MainConfig.Info.RedisSecret"), this.redisSecret));
		}

		final String redisChannelControlSuffix = properties.getProperty("redis.channel.control.suffix");
		if (redisChannelControlSuffix == null || redisChannelControlSuffix.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"), "redis.channel.control.suffix"));
			errors++;
		} else {
			this.redisChannelControl = this.redisSecret + redisChannelControlSuffix;
		}

		final String redisChannelRequestSuffix = properties.getProperty("redis.channel.request.suffix");
		if (redisChannelRequestSuffix == null || redisChannelRequestSuffix.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"), "redis.channel.request.suffix"));
			errors++;
		} else {
			this.redisChannelRequest = this.redisSecret + redisChannelRequestSuffix;
		}

		final String redisChannelResponseSuffix = properties.getProperty("redis.channel.response.suffix");
		if (redisChannelResponseSuffix == null || redisChannelResponseSuffix.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"), "redis.channel.response.suffix"));
			errors++;
		} else {
			this.redisChannelResponse = this.redisSecret + redisChannelResponseSuffix;
		}

		this.redisKeyRequestSuffix = properties.getProperty("redis.key.request.suffix");
		if (this.redisKeyRequestSuffix == null || this.redisKeyRequestSuffix.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"), "redis.key.requests.suffix"));
			errors++;
		}

		this.redisKeyResponseErrorSuffix = properties.getProperty("redis.key.response.error.suffix");
		if (this.redisKeyResponseErrorSuffix == null || this.redisKeyResponseErrorSuffix.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"),
					"redis.key.response.error.suffix"));
			errors++;
		}

		this.redisKeyResponseHeadersSuffix = properties.getProperty("redis.key.response.headers.suffix");
		if (this.redisKeyResponseHeadersSuffix == null || this.redisKeyResponseHeadersSuffix.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"),
					"redis.key.response.headers.suffix"));
			errors++;
		}

		this.redisKeyResponseSuffix = properties.getProperty("redis.key.response.suffix");
		if (this.redisKeyResponseSuffix == null || this.redisKeyResponseSuffix.isEmpty()) {
			log.error(String.format(Messages.getString("Error.PropertyMissingOrEmpty"), "redis.key.response.suffix"));
			errors++;
		}

		return errors == 0;
	}

}
