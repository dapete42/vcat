package org.toolforge.vcat.graphviz;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.params.GraphvizParams;

import java.nio.file.Path;
import java.util.concurrent.Semaphore;

@Slf4j
public class QueuedGraphviz implements Graphviz {

    /**
     * Semaphore to control number of concurrent executions.
     */
    @Nullable
    private final Semaphore semaphore;

    /*
     * Graphviz renderer used for actual rendering
     */
    private final Graphviz otherGraphviz;

    /**
     * Return an instance of QueuedGraphviz, which uses the supplied Graphviz for rendering.
     *
     * @param otherGraphviz           Graphviz renderer to use.
     * @param maxConcurrentExecutions Maximum number of concurrent executions (zero or less means no limit).
     */
    public QueuedGraphviz(Graphviz otherGraphviz, int maxConcurrentExecutions) {

        this.otherGraphviz = otherGraphviz;

        if (maxConcurrentExecutions < 1) {
            semaphore = null;
        } else {
            semaphore = new Semaphore(maxConcurrentExecutions);
        }

    }

    /**
     * @return The length of the queue for executions.
     */
    public int getQueueLength() {
        if (semaphore == null) {
            return 0;
        } else {
            return semaphore.getQueueLength();
        }
    }

    @Override
    public void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException {

        if (semaphore == null) {
            otherGraphviz.render(inputFile, outputFile, params);
        } else {
            try {
                semaphore.acquireUninterruptibly();
                otherGraphviz.render(inputFile, outputFile, params);
            } finally {
                semaphore.release();
            }
        }

    }

}
