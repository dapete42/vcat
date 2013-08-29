package vcat.toollabs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vcat.VCatException;

public class ToollabsMetainfoReader {

	Connection connection;

	public ToollabsMetainfoReader(final Connection connection) {
		this.connection = connection;
	}

	public ToollabsWiki wikiFor(final String dbname) throws VCatException {
		try {
			final PreparedStatement statement = connection.prepareStatement("SELECT * FROM wiki WHERE dbname=?");
			statement.setString(1, dbname);
			if (!statement.execute()) {
				throw new VCatException("Error reading Tool Labs meta information for dbname '" + dbname
						+ "': dbname not found");
			}
			ResultSet rs = statement.getResultSet();
			rs.first();
			final String lang = rs.getString("lang");
			final String name = rs.getString("name");
			final String url = rs.getString("url");
			rs.close();
			return new ToollabsWiki(dbname, lang, name, url);
		} catch (SQLException e) {
			throw new VCatException("Error reading Tool Labs meta information for dbname '" + dbname + '\'', e);
		}
	}

}
