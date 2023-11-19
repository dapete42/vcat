package org.toolforge.vcat.renderer;

import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.params.AbstractAllParams;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;
import org.toolforge.vcat.util.HashHelper;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class QueuedVCatRenderer implements VCatRenderer {

    @Serial
    private static final long serialVersionUID = 7817745519732907506L;

    private final ExecutorService executorService;

    /**
     * Map of Exceptions for jobs. If a job causes an exception, it will be saved in this Map.
     */
    private final Map<String, Exception> jobExceptions = new HashMap<>();

    /**
     * Map of all jobs. The value is an object we use to track the number of invocations waiting for each job.
     * <p>
     * This is also used to synchronize all operations on this Map or any of these other Collections to make the code
     * thread-safe.
     */
    private final Map<String, Integer> jobs = new HashMap<>();

    /**
     * Map of lock objects for each job.
     */
    private final Map<String, Object> jobLocks = new HashMap<>();

    /**
     * Map of finished Jobs and results for each job. Jobs are added to this when their Runnable instance has finished.
     */
    private final Map<String, RenderedFileInfo> jobsFinished = new HashMap<>();

    /**
     * Graphviz renderer used for actual rendering
     */
    private final VCatRenderer otherRenderer;

    /**
     * Return an instance of QueuedGraphviz, which uses the supplied Graphviz for rendering.
     *
     * @param otherRenderer   vCat renderer to use.
     * @param numberOfThreads Maximum number of threads to use (zero or less means an unlimited number).
     */
    public QueuedVCatRenderer(final VCatRenderer otherRenderer, final int numberOfThreads) {

        this.otherRenderer = otherRenderer;

        final ThreadFactory tf = Thread.ofVirtual()
                .name(getClass().getSimpleName() + '-' + hashCode())
                .factory();

        if (numberOfThreads < 1) {
            executorService = Executors.newCachedThreadPool(tf);
        } else if (numberOfThreads == 1) {
            executorService = Executors.newSingleThreadExecutor(tf);
        } else {
            executorService = Executors.newFixedThreadPool(numberOfThreads, tf);
        }
    }

    public int getNumberOfQueuedJobs() {
        return jobs.size();
    }

    @Override
    public RenderedFileInfo render(final AbstractAllParams all) throws VCatException {

        // Build a Job instance for the parameters.
        final String jobId = HashHelper.sha256Hex(all.getCombined());
        Object lock;

        // Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
        synchronized (jobs) {
            if (jobs.containsKey(jobId)) {
                // If the job is alread queued or running, we need to record that we are also waiting for it to finish.
                jobs.put(jobId, jobs.get(jobId) + 1);
                LOG.info(Messages.getString("QueuedVCatRenderer.Info.AlreadyScheduled"), jobId);
                // Get lock
                lock = jobLocks.get(jobId);
            } else {
                // If the job is not queued or running yet, it needs to be added to ths list and a new job started.
                jobs.put(jobId, 1);
                // Create new lock
                lock = new Object();
                jobLocks.put(jobId, lock);

                executorService.execute(() -> runJob(jobId, all));

                LOG.info(Messages.getString("QueuedVCatRenderer.Info.Scheduled"), jobId);
            }
        }

        synchronized (lock) {
            // Loop while waiting for the thread rendering the Job in the background.
            while (!jobsFinished.containsKey(jobId)) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
        synchronized (jobs) {

            // An Exception might have been thrown. Store it (or null if there was no Exception).
            Exception e = jobExceptions.get(jobId);

            // Get result
            RenderedFileInfo renderedFileInfo = jobsFinished.get(jobId);

            final int waiting = jobs.get(jobId);
            if (waiting > 1) {
                // If more than one is waiting, just record we are no longer waiting.
                jobs.put(jobId, waiting - 1);
            } else {
                // If no one else is still waiting, clear the Job.
                jobs.remove(jobId);
                jobsFinished.remove(jobId);
                jobLocks.remove(jobId);
                jobExceptions.remove(jobId);
            }
            // Throw exception, if there was one.
            if (e != null) {
                throw new VCatException(Messages.getString("QueuedVCatRenderer.Exception.Graphviz"), e);
            }

            // Return result
            return renderedFileInfo;

        }

    }

    /**
     * Called from the {@link Runnable#run()} implementation for the Job rendering thread.
     */
    private void runJob(final String jobId, final AbstractAllParams all) {

        LOG.info(Messages.getString("QueuedVCatRenderer.Info.ThreadStarted"), jobId);

        Object lock = jobLocks.get(jobId);
        synchronized (lock) {

            RenderedFileInfo renderedFileInfo = null;
            try {
                renderedFileInfo = otherRenderer.render(all);
            } catch (Exception e) {
                // Record exception as thrown for this job
                jobExceptions.put(jobId, e);
                LOG.error(Messages.getString("QueuedVCatRenderer.Exception.Job"), e);
            }

            // Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
            synchronized (jobs) {
                // Remove job from running jobs.
                jobsFinished.put(jobId, renderedFileInfo);
                lock.notifyAll();
            }

        }

        LOG.info(Messages.getString("QueuedVCatRenderer.Info.ThreadFinished"));

    }

}
