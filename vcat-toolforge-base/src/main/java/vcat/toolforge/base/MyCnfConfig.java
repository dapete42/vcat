package vcat.toolforge.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import vcat.VCatException;

public class MyCnfConfig {

	/** Log4j2 Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(MyCnfConfig.class);

	private static final String MY_CNF = "replica.my.cnf";

	private String user;

	private String password;

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public boolean readFromMyCnf() throws VCatException {

		Path myCnfFile = Paths.get(System.getProperty("user.home"), MY_CNF);

		Properties properties = new Properties();
		try (InputStream inputStream = Files.newInputStream(myCnfFile);
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
			properties.load(bufferedReader);
		} catch (IOException e) {
			throw new VCatException(
					MessageFormatter.format("Error reading file '{}'", myCnfFile.toAbsolutePath()).getMessage(), e);
		}

		int errors = 0;

		user = properties.getProperty("user");
		if (user == null || user.isEmpty()) {
			LOGGER.error("Property '{}' missing or empty", "user");
			errors++;
		}
		if (user != null && user.startsWith("'") && user.endsWith("'")) {
			user = user.substring(1, user.length() - 1);
		}

		password = properties.getProperty("password");
		if (password == null || password.isEmpty()) {
			LOGGER.error("Property '{}' missing or empty", "password");
			errors++;
		}
		if (password != null && password.startsWith("'") && password.endsWith("'")) {
			password = password.substring(1, password.length() - 1);
		}

		return errors == 0;
	}
}
