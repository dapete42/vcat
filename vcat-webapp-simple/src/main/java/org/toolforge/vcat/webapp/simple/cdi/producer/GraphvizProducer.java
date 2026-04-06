package org.toolforge.vcat.webapp.simple.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.graphviz.GraphvizJna;
import org.toolforge.vcat.graphviz.QueuedGraphviz;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.webapp.simple.cdi.ConfigProperties;

@ApplicationScoped
public class GraphvizProducer {

    @Inject
    ConfigProperties config;

    @Produces
    public Graphviz produceGraphviz() {
        final var baseGraphviz = new GraphvizJna();
        return new QueuedGraphviz(baseGraphviz, config.getGraphvizThreads());
    }

}
