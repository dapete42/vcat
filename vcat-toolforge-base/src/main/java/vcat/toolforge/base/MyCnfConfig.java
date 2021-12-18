package vcat.toolforge.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vcat.VCatException;

public class MyCnfConfig {

	/** Log4j2 Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(MyCnfConfig.class);

	private static final String MY_CNF = "replica.my.cnf";

	public String user;

	public String password;

	public boolean readFromMyCnf() throws VCatException {

		File myCnfFile = new File(System.getProperty("user.home"), MY_CNF);

		Properties properties = new Properties();
		try {
			BufferedReader propertiesReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(myCnfFile), StandardCharsets.UTF_8));
			properties.load(propertiesReader);
		} catch (IOException e) {
			throw new VCatException(String.format("Error reading file '%s'", myCnfFile.getAbsolutePath()), e);
		}

		int errors = 0;

		this.user = properties.getProperty("user");
		if (this.user == null || this.user.isEmpty()) {
			LOGGER.error(String.format("Property '%s' missing or empty", "user"));
			errors++;
		}
		if (this.user.startsWith("'") && this.user.endsWith("'")) {
			this.user = this.user.substring(1, this.user.length() - 1);
		}

		this.password = properties.getProperty("password");
		if (this.password == null || this.password.isEmpty()) {
			LOGGER.error(String.format("Property '%s' missing or empty", "password"));
			errors++;
		}
		if (this.password.startsWith("'") && this.password.endsWith("'")) {
			this.password = this.password.substring(1, this.password.length() - 1);
		}

		return errors == 0;
	}
}
