package org.toolforge.vcat.webapp.simple.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.graphviz.GraphvizExternal;
import org.toolforge.vcat.graphviz.QueuedGraphviz;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.webapp.simple.cdi.ConfigProperties;

import java.nio.file.Paths;

@ApplicationScoped
public class GraphvizProducer {

    @Inject
    ConfigProperties config;

    @Produces
    public Graphviz produceGraphviz() {
        final var graphvizDirPath = Paths.get(config.getGraphvizDir());
        final var baseGraphviz = new GraphvizExternal(graphvizDirPath);
        return new QueuedGraphviz(baseGraphviz, config.getGraphvizThreads());
    }

}
