package vcat.toollabs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import vcat.VCatException;
import vcat.toollabs.base.Messages;

public class ToollabsWikiProvider {

	private final ComboPooledDataSource cpds;

	public ToollabsWikiProvider(final ComboPooledDataSource cpds) {
		this.cpds = cpds;
	}

	public ToollabsWiki fromDbname(final String dbnameParam) throws VCatException {
		Connection connection = null;
		try {
			connection = cpds.getConnection();
			final PreparedStatement statement = connection.prepareStatement("SELECT * FROM wiki WHERE dbname=?");
			statement.setString(1, dbnameParam);
			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.first()) {
					rs.close();
					throw new VCatException(String.format(
							Messages.getString("ToollabsWikiProvider.Exception.DbnameNotFound"), dbnameParam));
				}
				final String dbname = rs.getString("dbname");
				final String name = rs.getString("name");
				final String url = rs.getString("url");
				return new ToollabsWiki(dbname, name, url);
			}
		} catch (SQLException e) {
			throw new VCatException(String.format(Messages.getString("ToollabsWikiProvider.Exception.ReadingMetaInfo"),
					dbnameParam), e);
		} finally {
			try {
				connection.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

}
