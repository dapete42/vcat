package org.toolforge.vcat.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
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

    private final Lock instanceLock = new ReentrantLock();

    private final Map<K, LockReference> lockReferenceMap = new HashMap<>();

    private final ReferenceQueue<ReentrantLock> lockReferenceQueue = new ReferenceQueue<>();

    /**
     * Return a lock already locked using {@link Lock#lock()}.
     *
     * @param key key
     * @return already locked lock
     */
    public ReentrantLock lock(K key) {
        processQueue();
        final var lock = getLock(key);
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
        instanceLock.lock();
        try {
            return lockReferenceMap.size();
        } finally {
            instanceLock.unlock();
        }
    }

    private ReentrantLock getLock(K key) {
        instanceLock.lock();
        try {
            final var lockReference = lockReferenceMap.get(key);
            final var lock = lockReference == null ? null : lockReference.get();
            if (lock == null) {
                final var newLock = new ReentrantLock();
                lockReferenceMap.put(key, new LockReference(key, newLock));
                return newLock;
            } else {
                return lock;
            }
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * Removes all locks that have been marked as unreachable by the garbage collector.
     */
    @SuppressWarnings("unchecked")
    private void processQueue() {
        instanceLock.lock();
        try {
            LockReference lockReference;
            while ((lockReference = (LockReference) lockReferenceQueue.poll()) != null) {
                lockReferenceMap.remove(lockReference.getKey());
            }
        } finally {
            instanceLock.unlock();
        }
    }

}
