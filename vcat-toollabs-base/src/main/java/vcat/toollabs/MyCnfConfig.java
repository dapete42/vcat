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

public class MyCnfConfig {

	private final Log log = LogFactory.getLog(this.getClass());

	private static final String MY_CNF = "replica.my.cnf";

	public String user;

	public String password;

	public boolean readFromMyCnf() throws VCatException {

		File myCnfFile = new File(System.getProperty("user.home"), MY_CNF);

		Properties properties = new Properties();
		try {
			BufferedReader propertiesReader = new BufferedReader(new InputStreamReader(new FileInputStream(myCnfFile),
					StandardCharsets.UTF_8));
			properties.load(propertiesReader);
		} catch (IOException e) {
			throw new VCatException(String.format("Error reading file '%s'", myCnfFile.getAbsolutePath()), e);
		}

		int errors = 0;

		this.user = properties.getProperty("user");
		if (this.user == null || this.user.isEmpty()) {
			log.error(String.format("Property '%s' missing or empty", "user"));
			errors++;
		}
		if (this.user.startsWith("'") && this.user.endsWith("'")) {
			this.user = this.user.substring(1, this.user.length() - 1);
		}

		this.password = properties.getProperty("password");
		if (this.password == null || this.password.isEmpty()) {
			log.error(String.format("Property '%s' missing or empty", "password"));
			errors++;
		}
		if (this.password.startsWith("'") && this.password.endsWith("'")) {
			this.password = this.password.substring(1, this.password.length() - 1);
		}

		return errors == 0;
	}
}
