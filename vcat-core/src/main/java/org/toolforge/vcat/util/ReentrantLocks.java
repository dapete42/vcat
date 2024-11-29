package org.toolforge.vcat.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Key-based locking based on {@link ReentrantLock}.
 *
 * @param <K> type of key
 */
public class ReentrantLocks<K> {

    private class LockReference extends WeakReference<ReentrantLock> {

        private final K key;

        private LockReference(K key, ReentrantLock lock) {
            super(lock, lockReferenceQueue);
            this.key = key;
        }

        private K getKey() {
            return key;
        }

    }

    private final Map<K, LockReference> lockReferenceMap = new ConcurrentHashMap<>();

    private final ReferenceQueue<ReentrantLock> lockReferenceQueue = new ReferenceQueue<>();

    private final Lock lockReferenceQueueLock = new ReentrantLock();

    /**
     * Return a lock already locked using {@link Lock#lock()}.
     *
     * @param key key
     * @return already locked lock
     */
    public ReentrantLock lock(K key) {
        processQueue();
        final var lock = Objects.requireNonNull(
                newLock(key)
        );
        lock.lock();
        return lock;
    }

    /**
     * Returns the current number of locks managed by this instance.
     *
     * @return number of locks
     */
    public int size() {
        processQueue();
        return lockReferenceMap.size();
    }

    private ReentrantLock newLock(K key) {
        return Objects.requireNonNull(
                lockReferenceMap.computeIfAbsent(key, this::newLockReference).get()
        );
    }

    private LockReference newLockReference(K key) {
        final var lock = new ReentrantLock();
        return new LockReference(key, lock);
    }

    /**
     * Removes all locks that have been marked as unreachable by the garbage collector.
     */
    @SuppressWarnings("unchecked")
    private void processQueue() {
        lockReferenceQueueLock.lock();
        try {
            LockReference lockReference;
            while ((lockReference = (LockReference) lockReferenceQueue.poll()) != null) {
                lockReferenceMap.remove(lockReference.getKey());
            }
        } finally {
            lockReferenceQueueLock.unlock();
        }
    }

}
