package vcat.toollabs.mediawiki;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vcat.VCatException;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IMetadataProvider;
import vcat.mediawiki.Metadata;
import vcat.toollabs.ToollabsWiki;

public class ToollabsCategoryProvider implements ICategoryProvider<ToollabsWiki> {

	private final ToollabsConnectionBuilder connectionBuilder;

	private final IMetadataProvider metadataProvider;

	public ToollabsCategoryProvider(final ToollabsConnectionBuilder connectionBuilder,
			final IMetadataProvider metadataProvider) {
		this.connectionBuilder = connectionBuilder;
		this.metadataProvider = metadataProvider;
	}

	@Override
	public Map<String, Collection<String>> requestCategories(ToollabsWiki wiki, Collection<String> fullTitles,
			boolean showhidden) throws ApiException {

		final String dbname = wiki.getName();
		Metadata metadata;
		try {
			metadata = metadataProvider.requestMetadata(wiki);
		} catch (ApiException e) {
			throw new ApiException("Could not retrieve metadata for wiki", e);
		}

		Connection connection;
		try {
			connection = this.connectionBuilder.buildConnection(dbname);
		} catch (VCatException e) {
			throw new ApiException("Could not get connection for requestCategories", e);
		}

		final HashMap<String, Collection<String>> result = new HashMap<String, Collection<String>>(fullTitles.size());
		for (final String fullTitle : fullTitles) {
			final String title = metadata.titleWithoutNamespace(fullTitle);
			final int namespace = metadata.namespaceFromTitle(fullTitle);
			final ArrayList<String> categoryFullTitles = new ArrayList<String>();
			try {
				StringBuilder sql = new StringBuilder(
						"SELECT cl_to FROM categorylinks INNER JOIN page ON page_id=cl_from"
								+ " WHERE page_namespace=? AND page_title=?");
				if (!showhidden) {
					sql.append(" AND NOT EXISTS (SELECT * FROM page_props WHERE pp_page=page_id AND pp_propname='hiddencat');");
				}
				final PreparedStatement statement = connection.prepareStatement(sql.toString());
				statement.setInt(1, namespace);
				statement.setString(2, title);
				ResultSet rs = statement.executeQuery();
				while (rs.next()) {
					final String cl_to = rs.getString("cl_to");
					categoryFullTitles.add(metadata.fullTitle(cl_to, Metadata.NS_CATEGORY));
				}
				rs.close();
			} catch (SQLException e) {
				throw new ApiException("Error reading categories from Tool Labs database '" + dbname + '\'', e);
			}
			if (!categoryFullTitles.isEmpty()) {
				result.put(fullTitle, categoryFullTitles);
			}
		}

		try {
			connection.close();
		} catch (SQLException e) {
			// ignore
		}

		return result;

	}

	@Override
	public List<String> requestCategorymembers(ToollabsWiki wiki, String fullTitle) throws ApiException {

		// TODO: Does not work yet.

		final String dbname = wiki.getName();
		Metadata metadata;
		try {
			metadata = this.metadataProvider.requestMetadata(wiki);
		} catch (ApiException e) {
			throw new ApiException("Could not retrieve metadata for wiki", e);
		}

		Connection connection;
		try {
			connection = this.connectionBuilder.buildConnection(dbname);
		} catch (VCatException e) {
			throw new ApiException("Could not get connection for requestCategories", e);
		}

		final ArrayList<String> result = new ArrayList<String>();

		final String title = metadata.titleWithoutNamespace(fullTitle);

		try {
			final PreparedStatement statement = connection
					.prepareStatement("SELECT page_title, page_namespace FROM page INNER JOIN categorylinks ON cl_from=page_id WHERE cl_to=?");
			statement.setString(1, title);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				final String page_title = rs.getString("page_title");
				final int page_namespace = rs.getInt("page_namespace");
				result.add(metadata.fullTitle(page_title, page_namespace));
			}
			rs.close();
		} catch (SQLException e) {
			throw new ApiException("Error reading category members from Tool Labs database '" + dbname + '\'', e);
		}

		try {
			connection.close();
		} catch (SQLException e) {
			// ignore
		}

		return result;
	}
}
