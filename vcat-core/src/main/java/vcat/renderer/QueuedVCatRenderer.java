package vcat.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import vcat.Messages;
import vcat.VCatException;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.util.HashHelper;

public class QueuedVCatRenderer<W extends IWiki> implements IVCatRenderer<W> {

	private final Log log = LogFactory.getLog(this.getClass());

	private final ExecutorService executorService;

	/**
	 * Map of Exceptions for jobs. If a job causes an exception, it will be saved in this Map.
	 */
	private final Map<String, Exception> jobExceptions = new HashMap<>();

	/**
	 * Map of all jobs. The value is an object we use to track the number number of invocations waiting for each job.
	 * <p>
	 * This is also used to synchronize all operations on this Map or any of these other Collections to make the code
	 * thread-safe.
	 */
	private final Map<String, Integer> jobs = new HashMap<>();

	/** Map of lock objects for each job. */
	private final Map<String, Object> jobLocks = new HashMap<>();

	/**
	 * Map of finished Jobs and results for each job. Jobs are added to this when their Runnable instance has finished.
	 */
	private final Map<String, RenderedFileInfo> jobsFinished = new HashMap<>();

	/** Graphviz renderer used for actual rendering */
	private final IVCatRenderer<W> otherRenderer;

	/**
	 * Return an instance of QueuedGraphviz, which uses the supplied Graphviz for rendering.
	 * 
	 * @param otherGraphviz
	 *            Graphviz renderer to use
	 * @param numberOfThreads
	 *            Maximum number of threads to use (Zero or less means an unlimited number)
	 */
	public QueuedVCatRenderer(final IVCatRenderer<W> otherRenderer, final int numberOfThreads) {

		this.otherRenderer = otherRenderer;

		final ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat(this.getClass().getSimpleName() + '-' + this.hashCode() + "-pool-%d");
		final ThreadFactory tf = tfb.build();

		if (numberOfThreads < 1) {
			this.executorService = Executors.newCachedThreadPool(tf);
		} else if (numberOfThreads == 1) {
			this.executorService = Executors.newSingleThreadExecutor(tf);
		} else {
			this.executorService = Executors.newFixedThreadPool(numberOfThreads, tf);
		}
	}

	@Override
	public RenderedFileInfo render(final AbstractAllParams<W> all) throws VCatException {

		// Build a Job instance for the parameters.
		final String jobId = HashHelper.hashFor(all.getCombined());
		Object lock;

		// Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
		synchronized (this.jobs) {
			if (this.jobs.containsKey(jobId)) {
				// If the job is alread queued or running, we need to record that we are also waiting for it to finish.
				this.jobs.put(jobId, this.jobs.get(jobId) + 1);
				log.info(String.format(Messages.getString("QueuedVCatRenderer.Info.AlreadyScheduled"), jobId));
				// Get lock
				lock = jobLocks.get(jobId);
			} else {
				// If the job is not queued or running yet, it needs to be added to ths list and a new job started.
				this.jobs.put(jobId, 1);
				// Create new lock
				lock = new Object();
				this.jobLocks.put(jobId, lock);

				this.executorService.execute(new Runnable() {

					@Override
					public void run() {
						runJob(jobId, all);
					}

				});

				log.info(String.format(Messages.getString("QueuedVCatRenderer.Info.Scheduled"), jobId));
			}
		}

		synchronized (lock) {
			// Loop while waiting for the thread rendering the Job in the background.
			while (!this.jobsFinished.containsKey(jobId)) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}

		// Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
		synchronized (this.jobs) {

			// An Exception might have been thrown. Store it (or null if there was no Exception).
			Exception e = this.jobExceptions.get(jobId);

			// Get result
			RenderedFileInfo renderedFileInfo = this.jobsFinished.get(jobId);

			final int waiting = this.jobs.get(jobId);
			if (waiting > 1) {
				// If more than one is waiting, just record we are no longer waiting.
				this.jobs.put(jobId, waiting - 1);
			} else {
				// If no one else is still waiting, clear the Job.
				this.jobs.remove(jobId);
				this.jobsFinished.remove(jobId);
				this.jobLocks.remove(jobId);
				this.jobExceptions.remove(jobId);
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
	private void runJob(final String jobId, final AbstractAllParams<W> all) {

		log.info(String.format(Messages.getString("QueuedVCatRenderer.Info.ThreadStarted"), jobId));

		Object lock = jobLocks.get(jobId);
		synchronized (lock) {

			RenderedFileInfo renderedFileInfo = null;
			try {
				renderedFileInfo = this.otherRenderer.render(all);
			} catch (Exception e) {
				// Record exception as thrown for this job
				this.jobExceptions.put(jobId, e);
				log.error(Messages.getString("QueuedVCatRenderer.Exception.Job"), e);
			}

			// Synchronized to jobs, as all code changing it or any of the other Collections storing Jobs.
			synchronized (this.jobs) {
				// Remove job from running jobs.
				this.jobsFinished.put(jobId, renderedFileInfo);
				lock.notifyAll();
			}

		}

		log.info(Messages.getString("QueuedVCatRenderer.Info.ThreadFinished"));

	}

}
