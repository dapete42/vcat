package vcat.graphviz;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vcat.Messages;
import vcat.graphviz.interfaces.Graphviz;
import vcat.params.GraphvizParams;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class QueuedGraphviz implements Graphviz {

    record Job(Path inputFile, Path outputFile, GraphvizParams params) {
    }

    /**
     * Log4j2 Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedGraphviz.class);

    private final ExecutorService executorService;

    /**
     * Map of Exceptions for jobs. If a job causes an exception during the call to
     * {@link Graphviz#render(Path, Path, GraphvizParams) Graphviz.render} (on the {@link Graphviz} instance passed in
     * the constructor), it will be saved in this Map and later thrown by {@link #render(Path, Path, GraphvizParams)}.
     */
    private final Map<Job, Exception> jobExceptions = new HashMap<>();

    /**
     * Map of all jobs. The value is an object we use to lockthe number number of calls to
     * {@link #render(Path, Path, GraphvizParams)} currently waiting for each Job. Each Job will be in this Map while
     * being processed.
     * <p>
     * This is also used to synchronize all operations on this Map or any of these other Collections to make the code
     * thread-safe.
     */
    private final Map<Job, Integer> jobs = new HashMap<>();

    /**
     * Map of lock objects for each job.
     */
    private final Map<Job, Object> jobLocks = new HashMap<>();

    /**
     * Set of finished Jobs. Jobs are added to this when their Runnable instance has finished. This causes calls to
     * {@link #render(Path, Path, GraphvizParams)} currently waiting for this Job to continue.
     */
    private final Set<Job> jobsFinished = new HashSet<>();

    /**
     * Graphviz renderer used for actual rendering
     */
    private final Graphviz otherGraphviz;

    /**
     * Return an instance of QueuedGraphviz, which uses the supplied Graphviz for rendering.
     *
     * @param otherGraphviz   Graphviz renderer to use
     * @param numberOfThreads Maximum number of threads to use (Zero or less means an unlimited number)
     */
    public QueuedGraphviz(Graphviz otherGraphviz, int numberOfThreads) {

        this.otherGraphviz = otherGraphviz;

        final ThreadFactory tf = new BasicThreadFactory.Builder()
                .namingPattern(this.getClass().getSimpleName() + '-' + this.hashCode() + "-pool-%d")
                .build();

        if (numberOfThreads < 1) {
            this.executorService = Executors.newCachedThreadPool(tf);
        } else if (numberOfThreads == 1) {
            this.executorService = Executors.newSingleThreadExecutor(tf);
        } else {
            this.executorService = Executors.newFixedThreadPool(numberOfThreads, tf);
        }
    }

    @Override
    public void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException {

        // Build a Job instance for the parameters.
        final Job job = new Job(inputFile, outputFile, params);
        Object lock;

        // Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
        synchronized (this.jobs) {
            if (this.jobs.containsKey(job)) {
                // If the job is alread queued or running, we need to record that we are also waiting for it to finish.
                this.jobs.put(job, this.jobs.get(job) + 1);
                LOGGER.info(Messages.getString("QueuedGraphviz.Info.AlreadyScheduled"), job.hashCode());
                // Get lock
                lock = jobLocks.get(job);
            } else {
                // If the job is not queued or running yet, it needs to be added to ths list and a new job started.
                this.jobs.put(job, 1);
                // Create new lock
                lock = new Object();
                this.jobLocks.put(job, lock);

                this.executorService.execute(() -> runJob(job));

                LOGGER.info(Messages.getString("QueuedGraphviz.Info.Scheduled"), job.hashCode());
            }
        }

        synchronized (lock) {
            // Loop while waiting for the thread rendering the Job in the background.
            while (!this.jobsFinished.contains(job)) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
        synchronized (this.jobs) {

            // An Exception might have been thrown. Store it (or null if there was no Exception).
            Exception e = this.jobExceptions.get(job);

            final int waiting = this.jobs.get(job);
            if (waiting > 1) {
                // If more than one is waiting, just record we are no longer waiting.
                this.jobs.put(job, waiting - 1);
            } else {
                // If no one else is still waiting, clear the Job.
                this.jobs.remove(job);
                this.jobsFinished.remove(job);
                this.jobLocks.remove(job);
                this.jobExceptions.remove(job);
            }
            // Throw exception, if there was one.
            if (e != null) {
                throw new GraphvizException(Messages.getString("QueuedGraphviz.Exception.Graphviz"), e);
            }

        }

    }

    /**
     * Called from the {@link Runnable#run()} implementation for the Job rendering thread.
     */
    private void runJob(Job job) {

        LOGGER.info(Messages.getString("QueuedGraphviz.Info.ThreadStarted"), job.hashCode());

        Object lock = jobLocks.get(job);
        synchronized (lock) {

            try {
                this.otherGraphviz.render(job.inputFile, job.outputFile, job.params);
            } catch (Exception e) {
                // Record exception as thrown for this job
                this.jobExceptions.put(job, e);
                LOGGER.error(Messages.getString("QueuedGraphviz.Exception.Job"), e);
            }

            // Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
            synchronized (this.jobs) {
                // Remove job from running jobs.
                this.jobsFinished.add(job);
                lock.notifyAll();
            }

        }

        LOGGER.info(Messages.getString("QueuedGraphviz.Info.ThreadFinished"));

    }

}
