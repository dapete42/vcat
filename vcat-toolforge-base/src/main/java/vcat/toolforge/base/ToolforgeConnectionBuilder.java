package vcat.toolforge.base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import vcat.VCatException;

public class ToolforgeConnectionBuilder {

	private final String jdbcPassword;

	private final String jdbcUrlTemplate;

	private final String jdbcUser;

	public ToolforgeConnectionBuilder(final String jdbcUser, final String jdbcPassword, final String jdbcUrlTemplate) {
		this.jdbcUser = jdbcUser;
		this.jdbcPassword = jdbcPassword;
		this.jdbcUrlTemplate = jdbcUrlTemplate;
	}

	public Connection buildConnection(final String name) throws VCatException {
		final String jdbcUrl = String.format(this.jdbcUrlTemplate, name + ".web.db.svc.wikimedia.cloud", name + "_p");
		try {
			return DriverManager.getConnection(jdbcUrl, this.jdbcUser, this.jdbcPassword);
		} catch (SQLException e) {
			throw new VCatException(String.format(Messages.getString("Exception.Database"), jdbcUrl), e);
		}
	}

}
