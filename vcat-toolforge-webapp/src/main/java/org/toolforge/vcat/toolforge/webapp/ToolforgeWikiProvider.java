package org.toolforge.vcat.toolforge.webapp;

import org.slf4j.helpers.MessageFormatter;
import org.toolforge.vcat.VCatException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ToolforgeWikiProvider {

    public static final String WIKI_DBNAME_QUERY = "SELECT * FROM wiki WHERE dbname=?";

    private final DataSource dataSource;

    public ToolforgeWikiProvider(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ToolforgeWiki fromDbname(final String dbnameParam) throws VCatException {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(WIKI_DBNAME_QUERY)) {
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
