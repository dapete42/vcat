package org.toolforge.vcat.renderer;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.params.AbstractAllParams;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;

import java.io.Serial;
import java.util.concurrent.Semaphore;

@Slf4j
public class QueuedVCatRenderer implements VCatRenderer {

    @Serial
    private static final long serialVersionUID = 5848442969269137328L;
    /**
     * Semaphore to control number of concurrent executions.
     */
    @Nullable
    private final Semaphore semaphore;

    /**
     * Graphviz renderer used for actual rendering
     */
    private final VCatRenderer otherRenderer;

    /**
     * Return an instance of QueuedGraphviz, which uses the supplied Graphviz for rendering.
     *
     * @param otherRenderer           vCat renderer to use.
     * @param maxConcurrentExecutions Maximum number of concurrent executions (zero or less means no limit).
     */
    public QueuedVCatRenderer(VCatRenderer otherRenderer, int maxConcurrentExecutions) {

        this.otherRenderer = otherRenderer;

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
    public RenderedFileInfo render(AbstractAllParams all) throws VCatException {

        if (semaphore == null) {
            return otherRenderer.render(all);
        } else {
            try {
                semaphore.acquireUninterruptibly();
                return otherRenderer.render(all);
            } finally {
                semaphore.release();
            }
        }

    }

}
