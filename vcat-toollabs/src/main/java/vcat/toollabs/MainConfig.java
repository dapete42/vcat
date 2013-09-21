package vcat.toollabs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.VCatException;

public class MainConfig {

	private static final Log log = LogFactory.getLog(MainConfig.class);

	public String cacheDir;

	public String jdbcUrl;

	public String jdbcUser;

	public String jdbcPassword;

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
					propertiesFile), "UTF8"));
			properties.load(propertiesReader);
		} catch (IOException e) {
			throw new VCatException("Error reading .properties file '" + propertiesFile.getAbsolutePath() + "'", e);
		}

		int errors = 0;

		cacheDir = properties.getProperty("cache.dir");
		if (cacheDir == null || cacheDir.isEmpty()) {
			log.error("Property cache.dir missing or empty");
			errors++;
		}

		jdbcUrl = properties.getProperty("jdbc.url");
		if (jdbcUrl == null || jdbcUrl.isEmpty()) {
			log.error("Property jdbc.url missing or empty");
			errors++;
		}

		jdbcUser = properties.getProperty("jdbc.user");
		if (jdbcUser == null || jdbcUser.isEmpty()) {
			log.error("Property jdbc.user missing or empty");
			errors++;
		}

		jdbcPassword = properties.getProperty("jdbc.password");
		if (jdbcPassword == null || jdbcPassword.isEmpty()) {
			log.error("Property jdbc.password missing or empty");
			errors++;
		}

		final String purgeString = properties.getProperty("purge");
		if (purgeString == null || purgeString.isEmpty()) {
			purge = 60;
			log.info("Property purge not set, using default value " + purge);
		} else {
			try {
				purge = Integer.parseInt(purgeString);
			} catch (NumberFormatException e) {
				log.error("Property purge is not a number", e);
				errors++;
			}
		}

		final String purgeMetadataString = properties.getProperty("purge.metadata");
		if (purgeMetadataString == null || purgeMetadataString.isEmpty()) {
			purgeMetadata = 60;
			log.info("Property purge.metadata not set, using default value " + purgeMetadata);
		} else {
			try {
				purgeMetadata = Integer.parseInt(purgeMetadataString);
			} catch (NumberFormatException e) {
				log.error("Property purge.metadata is not a number", e);
				errors++;
			}
		}

		redisServerHostname = properties.getProperty("redis.server.hostname");
		if (redisServerHostname == null) {
			log.error("Property redis.server.hostname missing");
			errors++;
		}

		final String redisServerPortString = properties.getProperty("redis.server.port");
		if (redisServerPortString == null || redisServerPortString.isEmpty()) {
			redisServerPort = 6379;
			log.info("Property redis.server.port not set, using default value " + redisServerPort);
		} else {
			try {
				redisServerPort = Integer.parseInt(redisServerPortString);
			} catch (NumberFormatException e) {
				log.error("Property redis.server.port is not a number", e);
				errors++;
			}
		}

		this.redisSecret = properties.getProperty("redis.secret");
		if (this.redisSecret == null) {
			log.error("Property redis.secret missing");
			errors++;
		} else {
			log.info("Using redis secret " + this.redisSecret);
		}

		final String redisChannelControlSuffix = properties.getProperty("redis.channel.control.suffix");
		if (redisChannelControlSuffix == null || redisChannelControlSuffix.isEmpty()) {
			log.error("Property redis.channel.control.suffix missing or empty");
			errors++;
		} else {
			this.redisChannelControl = this.redisSecret + redisChannelControlSuffix;
		}

		final String redisChannelRequestSuffix = properties.getProperty("redis.channel.request.suffix");
		if (redisChannelRequestSuffix == null || redisChannelRequestSuffix.isEmpty()) {
			log.error("Property redis.channel.request.suffix missing or empty");
			errors++;
		} else {
			this.redisChannelRequest = this.redisSecret + redisChannelRequestSuffix;
		}

		final String redisChannelResponseSuffix = properties.getProperty("redis.channel.response.suffix");
		if (redisChannelResponseSuffix == null || redisChannelResponseSuffix.isEmpty()) {
			log.error("Property redis.channel.response.suffix missing or empty");
			errors++;
		} else {
			this.redisChannelResponse = this.redisSecret + redisChannelResponseSuffix;
		}

		this.redisKeyRequestSuffix = properties.getProperty("redis.key.request.suffix");
		if (this.redisKeyRequestSuffix == null || this.redisKeyRequestSuffix.isEmpty()) {
			log.error("Property redis.key.requests.suffix missing or empty");
			errors++;
		}

		this.redisKeyResponseErrorSuffix = properties.getProperty("redis.key.response.error.suffix");
		if (this.redisKeyResponseErrorSuffix == null || this.redisKeyResponseErrorSuffix.isEmpty()) {
			log.error("Property redis.key.response.error.suffix missing or empty");
			errors++;
		}

		this.redisKeyResponseHeadersSuffix = properties.getProperty("redis.key.response.headers.suffix");
		if (this.redisKeyResponseHeadersSuffix == null || this.redisKeyResponseHeadersSuffix.isEmpty()) {
			log.error("Property redis.key.response.headers.suffix missing or empty");
			errors++;
		}

		this.redisKeyResponseSuffix = properties.getProperty("redis.key.response.suffix");
		if (this.redisKeyResponseSuffix == null || this.redisKeyResponseSuffix.isEmpty()) {
			log.error("Property redis.key.response.suffix missing or empty");
			errors++;
		}

		return errors == 0;
	}

}
