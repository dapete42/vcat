package vcat.toollabs.base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import vcat.VCatException;

public class ToollabsConnectionBuilder {

	private static final String JDBC_URL_TEMPLATE = "jdbc:mysql://%s:3306/%s";

	private final String jdbcPassword;

	private final String jdbcUser;

	public ToollabsConnectionBuilder(final String jdbcUser, final String jdbcPassword) {
		this.jdbcUser = jdbcUser;
		this.jdbcPassword = jdbcPassword;
	}

	public Connection buildConnection(final String name) throws VCatException {
		final String jdbcUrl = String.format(JDBC_URL_TEMPLATE, name + ".labsdb", name + "_p");
		try {
			return DriverManager.getConnection(jdbcUrl, this.jdbcUser, this.jdbcPassword);
		} catch (SQLException e) {
			throw new VCatException(String.format(Messages.getString("Exception.Database"), jdbcUrl), e);
		}
	}

}
