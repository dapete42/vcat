package org.toolforge.vcat.toolforge.webapp.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.toolforge.webapp.ToolforgeWikiProvider;

import javax.sql.DataSource;

@ApplicationScoped
public class ToolforgeWikiProviderProducer {

    /**
     * Injected Quarkus data source
     */
    @Inject
    DataSource dataSource;

    @Produces
    public ToolforgeWikiProvider produceToolforgeWikiProvider() {
        return new ToolforgeWikiProvider(dataSource);
    }

}
