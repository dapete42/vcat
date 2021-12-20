package vcat.toolforge.base;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.helpers.MessageFormatter;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import vcat.VCatException;

public class ToolforgeWikiProvider {

	private final ComboPooledDataSource cpds;

	public ToolforgeWikiProvider(final ComboPooledDataSource cpds) {
		this.cpds = cpds;
	}

	public ToolforgeWiki fromDbname(final String dbnameParam) throws VCatException {
		try (Connection connection = this.cpds.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM wiki WHERE dbname=?")) {
			statement.setString(1, dbnameParam);
			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.first()) {
					throw new VCatException(MessageFormatter
							.format(Messages.getString("ToolforgeWikiProvider.Exception.DbnameNotFound"), dbnameParam)
							.getMessage());
				}
				final String dbname = rs.getString("dbname");
				final String name = rs.getString("name");
				final String url = rs.getString("url");
				return new ToolforgeWiki(dbname, name, url);
			}
		} catch (SQLException e) {
			throw new VCatException(MessageFormatter
					.format(Messages.getString("ToolforgeWikiProvider.Exception.ReadingMetaInfo"), dbnameParam)
					.getMessage(), e);
		}
	}

}
