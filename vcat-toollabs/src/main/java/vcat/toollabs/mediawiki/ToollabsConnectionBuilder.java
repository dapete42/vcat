package vcat.toollabs.mediawiki;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import vcat.VCatException;

public class ToollabsConnectionBuilder {

	private final String jdbcPassword;

	/**
	 * Template for the jdbc connection URL. Must be in the String format used by
	 * {@link String#format(String, Object...)}, containing two String placeholders (<code>%s</code>), the first for the
	 * host name, the second for the database name to connect to.
	 */
	private final String jdbcUrlTemplate;

	private final String jdbcUser;

	public ToollabsConnectionBuilder(final String jdbcUrlTemplate, final String jdbcUser, final String jdbcPassword) {
		this.jdbcUrlTemplate = jdbcUrlTemplate;
		this.jdbcUser = jdbcUser;
		this.jdbcPassword = jdbcPassword;
	}

	public Connection buildConnection(final String name) throws VCatException {
		final String jdbcUrl = String.format(jdbcUrlTemplate, name + ".labsdb", name + "_p");
		try {
			return DriverManager.getConnection(jdbcUrl, this.jdbcUser, this.jdbcPassword);
		} catch (SQLException e) {
			throw new VCatException("Error connecting to database url '" + jdbcUrl + '\'', e);
		}
	}

}
