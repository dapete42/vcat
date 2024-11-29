package org.toolforge.vcat.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ReentrantLocksTest {

    @Test
    void testLocking() throws Exception {
        final ReentrantLocks<Integer> locks = new ReentrantLocks<>();

        AtomicBoolean threadHasStarted = new AtomicBoolean(false);
        AtomicBoolean threadHasLocked = new AtomicBoolean(false);

        var lock = locks.lock(1);
        try {

            Runnable runnable = () -> {
                threadHasStarted.set(true);
                var l = locks.lock(1);
                try {
                    threadHasLocked.set(true);
                } finally {
                    l.unlock();
                }
            };
            new Thread(runnable).start();

            while (!threadHasStarted.get()) {
                Thread.yield();
            }

            TimeUnit.SECONDS.sleep(1);

            assertFalse(threadHasLocked.get());

        } finally {
            lock.unlock();
        }

        TimeUnit.SECONDS.sleep(1);

        assertTrue(threadHasLocked.get());
    }

    @Test
    void testLocksAreReleasedWhenUnused() throws Exception {
        final ReentrantLocks<Integer> locks = new ReentrantLocks<>();

        var lock = locks.lock(1);
        lock.unlock();

        int size = locks.size();
        assertEquals(1, size);

        // wait up to 30 seconds for size to change after dereferencing the lock
        lock = null;
        for (int i = 30; i > 0 && size > 0; i--) {
            // purely for encouragement, not guaranteed to do anything
            System.gc();
            TimeUnit.SECONDS.sleep(1);
            size = locks.size();
        }
        assertEquals(0, size);
    }

}
