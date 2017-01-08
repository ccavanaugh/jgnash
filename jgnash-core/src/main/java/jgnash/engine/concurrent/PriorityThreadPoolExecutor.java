/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.concurrent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Decorator around a {@code ThreadPoolExecutor} that provides execution priority.
 *
 * Callables with the same priority level are FIFO'd.
 *
 * @author Craig Cavanaugh
 */
public class PriorityThreadPoolExecutor {

    private final ThreadPoolExecutor threadPoolExecutor;

    private final PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>();

    public PriorityThreadPoolExecutor(final int poolSize) {
        threadPoolExecutor = new ThreadPoolExecutor(1, poolSize, 1, TimeUnit.MINUTES, queue) {

            // Wraps a Callable with a FutureTaskWrapper that respects the Priority
            @Override
            protected <v> RunnableFuture<v> newTaskFor(final Callable<v> c) {
                return new FutureTaskWrapper<>((PriorityCallable<v>) c);
            }
        };

        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    private  <T> Future<T> submit(final Callable<T> callable, final Priority priority) {
        return threadPoolExecutor.submit(new PriorityCallable<T>() {
            @Override
            public Priority getPriority() {
                return priority;
            }

            @Override
            public T call() throws Exception {
                return callable.call();
            }
        });
    }

    public <T> Future<T> submit(final Callable<T> callable, final int priority) {
        return submit(callable, new Priority(priority));
    }

    public <T> Future<T> submit(final Callable<T> callable) {
        return submit(callable, Priority.SYSTEM);
    }

    public void shutdown() {

        /* Remove any non-critical system tasks from the executor first */
        for (final Runnable runnable : queue.toArray(new Runnable[0])) {
            if (((FutureTaskWrapper)runnable).getPriorityCallable().getPriority().getPriority() != Priority.SYSTEM) {
                threadPoolExecutor.remove(runnable);
            }
        }

        threadPoolExecutor.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return threadPoolExecutor.shutdownNow();
    }

    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return threadPoolExecutor.awaitTermination(timeout, unit);
    }

    //A future task that wraps around the priority task to be used in the queue
    static class FutureTaskWrapper<T> extends FutureTask<T> implements Comparable<FutureTaskWrapper<T>> {
        private PriorityCallable<T> priorityCallable;

        FutureTaskWrapper(final PriorityCallable<T> priorityCallable) {
            super(priorityCallable);
            this.priorityCallable = priorityCallable;
        }

        PriorityCallable<T> getPriorityCallable() {
            return priorityCallable;
        }

        @Override
        public int compareTo(final FutureTaskWrapper<T> other) {
            return priorityCallable.getPriority().compareTo(other.priorityCallable.getPriority());
        }
    }

    private interface PriorityCallable<V> extends Callable<V> {
        Priority getPriority ();
    }
}




