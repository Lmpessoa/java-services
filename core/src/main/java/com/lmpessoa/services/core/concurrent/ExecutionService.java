/*
 * Copyright (c) 2018 Leonardo Pessoa
 * https://github.com/lmpessoa/java-services
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lmpessoa.services.core.concurrent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.lmpessoa.services.util.logging.ILogger;

/**
 * Provides methods to execute a large number of asynchronous tasks.
 *
 * <p>
 * An {@code ExecutionService} provides means to coordinate the execution of large numbers of
 * asynchronous tasks with improved performance over manually running each in a separate thread.
 * </p>
 *
 * <p>
 * There are two major problems an {#code ExecutionService} solves that are not addressed by other
 * executor services provided by the Java language: they assign an unique identifier to each task
 * being submitted to execution which, in turn, allow for the results to retrieved later by using
 * these identifiers. Also, during execution, worker threads assume the identity of the running task
 * enabling log entries to have access to that information.
 * </p>
 */
public final class ExecutionService implements IExecutionService {

   private final Map<String, AtomicLong> prefixes = new ConcurrentHashMap<>();
   private final Map<String, Task<?>> tasks = new ConcurrentHashMap<>();
   private final AtomicBoolean shutdown = new AtomicBoolean(false);
   private final Queue<Task<?>> queue = new LinkedBlockingQueue<>();
   private final List<Worker> workers = new ArrayList<>();
   private final int maxWorkerCount;
   private final ILogger log;

   private long resultRetentionTimeout = TimeUnit.HOURS.toNanos(3);
   private long keepAliveTime = TimeUnit.MINUTES.toNanos(1);

   /**
    * Creates a new {@code ExecutionService} with the maximum number of concurrent jobs.
    *
    * @param maxConcurrentJobs the maximum number of concurrent tasks this {@code ExecutorService}
    *           can execute. If this number is zero or negative, the service will not limit the
    *           amount of tasks to execute concurrently.
    * @param log a logger used to register issues while running tasks.
    */
   public ExecutionService(int maxConcurrentJobs, ILogger log) {
      this.maxWorkerCount = maxConcurrentJobs > 0 ? maxConcurrentJobs : 0;
      this.log = log;
   }

   @Override
   public Future<?> get(String jobId) {
      purgeExpired();
      return tasks.get(jobId);
   }

   @Override
   public Set<String> keySet() {
      purgeExpired();
      return tasks.keySet();
   }

   @Override
   public boolean isShutdown() {
      return shutdown.get();
   }

   /**
    * Returns whether all tasks have completed following a shut down.
    *
    * <p>
    * Note that this method will never {@code true} unless either {@link #shutdown()} or
    * {@link #shutdown(boolean)} was called first.
    * </p>
    *
    * @return {@code true} if all tasks have completed following shut down.
    */
   public boolean isTerminated() {
      return shutdown.get() && workers.isEmpty();
   }

   /**
    * Shuts down this {@code ExecutionService}.
    *
    * <p>
    * After this method is executed, an {@code ExecutionService} will cancel any tasks not yet in
    * executions and refuse any new tasks but will not attemp to interrupt any already running tasks
    * pending completion.
    * </p>
    *
    * <p>
    * This method does not wait for previously submitted tasks to complete execution. Use
    * {@link #isTerminated()} to do that.
    * </p>
    *
    * <p>
    * After this call, results of tasks not yet purged will still be available until they expire.
    * </p>
    */
   public void shutdown() {
      shutdown(false);
   }

   /**
    * Shuts down this {@code ExecutionService}.
    *
    * <p>
    * After this method is executed, an {@code ExecutionService} will cancel any tasks not yet in
    * executions and refuse any new tasks. If the value of {@code mayInterrupRunning} is
    * {@code true}, it will attempt to interrupt any tasks currently running, but there is no
    * guarantee they will be terminated.
    * </p>
    *
    * <p>
    * This method does not wait for previously submitted tasks to complete execution. Use
    * {@link #isTerminated()} to do that.
    * </p>
    *
    * <p>
    * After this call, results of tasks not yet purged will still be available until they expire.
    * </p>
    *
    * @param mayInterruptRunning {@code true} if the execution service should attempt to interrupt
    *           any running tasks, or {@code false} to allow running tasks to complete on their own.
    */
   public void shutdown(boolean mayInterruptRunning) {
      if (shutdown.get()) {
         return;
      }
      shutdown.set(true);
      tasks.values().stream().filter(j -> j.status == Status.QUEUED).forEach(
               j -> j.status = Status.CANCELLED);
      if (mayInterruptRunning) {
         tasks.values().stream().filter(j -> j.status == Status.RUNNING).forEach(j -> {
            j.status = Status.INTERRUPTED;
            j.worker.interrupt();
         });
      }
   }

   /**
    * Submits a Runnable task for execution and returns an identifier for that task.
    *
    * <p>
    * The returned identifier can be used at any moment to obtain that task's Future result, which
    * in turn can be used to check if the task has been completed and what result it returned.
    * </p>
    *
    * @param task the task to be executed.
    * @return an identifier that uniquely identifies this task.
    * @throws RejectedExecutionException if the task cannot be scheduled for execution.
    * @throws NullPointerException if the task is null.
    */
   public String submit(Runnable task) {
      return submit(Executors.callable(task));
   }

   /**
    * Submits a Runnable task for execution and returns an identifier for that task.
    *
    * <p>
    * The returned identifier is composed of a sequential number with the given prefix and can be
    * used at any moment to obtain that task's Future result, which in turn can be used to check if
    * the task has been completed and what result it returned.
    * </p>
    *
    * @param task the task to be executed.
    * @param prefix a prefix to be used to generate a unique identifier for this task.
    * @return an identifier that uniquely identifies this task.
    * @throws RejectedExecutionException if the task cannot be scheduled for execution.
    * @throws NullPointerException if the task is null.
    */
   public String submit(Runnable task, String prefix) {
      return submit(Executors.callable(task), prefix);
   }

   /**
    * Submits a Callable task for execution and returns an identifier for that task.
    *
    * <p>
    * The returned identifier is composed of a sequential number with the given prefix and can be
    * used at any moment to obtain that task's Future result, which in turn can be used to check if
    * the task has been completed and what result it returned.
    * </p>
    *
    * @param task the task to be executed.
    * @return an identifier that uniquely identifies this task.
    * @throws RejectedExecutionException if the task cannot be scheduled for execution.
    * @throws NullPointerException if the task is null.
    */
   public <T> String submit(Callable<T> task) {
      if (shutdown.get()) {
         throw new RejectedExecutionException();
      }
      Task<?> rtask = new Task<>(task);
      String id = store(rtask);
      executeOrQueue(rtask);
      return id;
   }

   /**
    * Submits a Callable task for execution and returns an identifier for that task.
    *
    * <p>
    * The returned identifier is composed of a sequential number with the given prefix and can be
    * used at any moment to obtain that task's Future result, which in turn can be used to check if
    * the task has been completed and what result it returned.
    * </p>
    *
    * @param task the task to be executed.
    * @param prefix a prefix to be used to generate a unique identifier for this task.
    * @return an identifier that uniquely identifies this task.
    * @throws RejectedExecutionException if the task cannot be scheduled for execution.
    * @throws NullPointerException if the task is null.
    */
   public <T> String submit(Callable<T> task, String prefix) {
      if (shutdown.get()) {
         throw new RejectedExecutionException();
      }
      Task<?> rtask = new Task<>(task);
      String id = store(rtask, prefix);
      executeOrQueue(rtask);
      return id;
   }

   /**
    * Returns the thread keep-alive time.
    *
    * <p>
    * The thread keep-alive time is the amount of time that threads may remain idle before being
    * terminated.
    * </p>
    *
    * @param unit the desired time unit of the result.
    * @return the time limit.
    * @see #setKeepAliveTime(long, TimeUnit)
    */
   public long getKeepAliveTime(TimeUnit unit) {
      return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
   }

   /**
    * Sets the time limit for which threads may remain idle before being terminated.
    *
    * <p>
    * If there are no more tasks to be executed in this queue, after waiting this amount of time
    * without processing a task, a thread will be terminated.
    * </p>
    *
    * @param timeout the time to wait. A time value of zero will cause excess threads to terminate
    *           immediately after executing tasks.
    * @param unit the time unit of the {@code timeout} argument.
    * @throws IllegalArgumentException if {@code timeout} os less than zero.
    * @see #getKeepAliveTime(TimeUnit)
    */
   public void setKeepAliveTimout(long timeout, TimeUnit unit) {
      if (timeout < 0) {
         throw new IllegalArgumentException();
      }
      keepAliveTime = unit.toNanos(timeout);
   }

   /**
    * Returns the result retention time.
    *
    * <p>
    * The result retention time is the amount of time that results of completed tasks will be kept
    * before being purged.
    * </p>
    *
    * @param unit the desired time unit of the result.
    * @return the time limit.
    * @see #setResultRetentionTimeout(long, TimeUnit)
    */
   public long getResultRetentionTimeout(TimeUnit unit) {
      return unit.convert(resultRetentionTimeout, TimeUnit.NANOSECONDS);
   }

   /**
    * Sets the time limit for which results will be retained before being purged.
    *
    * @param timeout the time to wait. A time value of zero will cause results to be purged
    *           immediately after executing tasks.
    * @param unit the time unit of the {@code timeout} argument.
    * @throws IllegalArgumentException if {@code timeout} is less than zero.
    */
   public void setResultRetentionTimeout(long timeout, TimeUnit unit) {
      if (timeout < 0) {
         throw new IllegalArgumentException();
      }
      resultRetentionTimeout = unit.toNanos(timeout);
   }

   private String store(Task<?> task) {
      String key;
      synchronized (tasks) {
         do {
            key = UUID.randomUUID().toString();
         } while (tasks.containsKey(key));
         tasks.put(key, task);
      }
      return key;
   }

   private String store(Task<?> task, String prefix) {
      if (!prefixes.containsKey(prefix)) {
         prefixes.put(prefix, new AtomicLong(0));
      }
      String key = String.format("%s-%d", prefix, prefixes.get(prefix).incrementAndGet());
      tasks.put(key, task);
      return key;
   }

   private void executeOrQueue(Task<?> task) {
      if (workers.stream().anyMatch(w -> w.task == null)
               || maxWorkerCount > 0 && workers.size() >= maxWorkerCount) {
         queue.add(task);
      } else {
         Worker worker = new Worker(task);
         workers.add(worker);
         worker.start();
      }
   }

   private void purgeExpired() {
      Instant limit = Instant.now().minusNanos(resultRetentionTimeout);
      tasks.entrySet().stream().filter(e -> {
         Instant completed = e.getValue().completed;
         return completed != null && completed.isBefore(limit);
      }).forEach(e -> tasks.remove(e.getKey()));
   }

   private enum Status {
      QUEUED, RUNNING, DONE, FAILED, CANCELLED, INTERRUPTED;
   }

   private final class Task<T> implements Future<T> {

      // Fields are marked transient so they do not get serialised
      private final transient Callable<T> job; // NOSONAR
      private transient Worker worker = null; // NOSONAR
      private transient Throwable exception = null; // NOSONAR
      private transient Instant completed; // NOSONAR

      // Used only by serialisation to JSON/XML
      @SuppressWarnings("unused")
      private String error = null;

      private Status status = Status.QUEUED;
      private T result = null;

      public Task(Callable<T> job) {
         this.job = Objects.requireNonNull(job);
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         switch (status) {
            case QUEUED:
               queue.remove(this);
               status = Status.CANCELLED;
               return true;
            case RUNNING:
               if (!mayInterruptIfRunning) {
                  return false;
               }
               worker.interrupt();
               status = Status.INTERRUPTED;
               return true;
            default:
               return false;
         }
      }

      @Override
      public boolean isCancelled() {
         return status == Status.CANCELLED || status == Status.INTERRUPTED;
      }

      @Override
      public boolean isDone() {
         return status != Status.QUEUED && status != Status.RUNNING;
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
         try {
            return get(49, TimeUnit.DAYS);
         } catch (TimeoutException e) {
            // No application might be running for 49 days straight so ignore
         }
         return null;
      }

      @Override
      public T get(long timeout, TimeUnit unit)
         throws InterruptedException, ExecutionException, TimeoutException {
         Instant end = Instant.now().plusNanos(unit.toNanos(timeout));
         while (!isDone()) {
            if (end.isBefore(Instant.now())) {
               throw new TimeoutException();
            }
         }
         switch (status) {
            case FAILED:
               throw new ExecutionException(exception);
            case CANCELLED:
               throw new CancellationException();
            case INTERRUPTED:
               throw new InterruptedException();
            default:
               // Will never get here
               return result;
         }
      }

      void run() {
         if (completed == null) {
            try {
               status = Status.RUNNING;
               result = job.call();
               status = Status.DONE;
            } catch (InterruptedException e) {
               status = Status.INTERRUPTED;
               log.warning(e);
               Thread.currentThread().interrupt();
            } catch (Throwable t) { // NOSONAR
               status = Status.FAILED;
               error = t.getMessage();
               exception = t;
               log.error(t);
            } finally {
               completed = Instant.now();
            }
         }
      }
   }

   private final class Worker extends Thread {

      private Task<?> task;

      public Worker(Task<?> firstTask) {
         this.task = firstTask;
      }

      @Override
      public void run() {
         while (task != null) {
            setName(getIdOf(task));
            task.worker = this;
            task.run();
            task.worker = null;
            task = timedPoll();
         }
         workers.remove(this);
      }

      private Task<?> timedPoll() {
         Instant end = Instant.now().plusNanos(keepAliveTime);
         while (end.isAfter(Instant.now())) {
            Task<?> result = queue.poll();
            if (result != null) {
               return result;
            }
            try {
               Thread.sleep(60);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return null;
            }
         }
         return null;
      }

      private String getIdOf(Task<?> task) {
         Optional<String> id = tasks.entrySet()
                  .stream()
                  .filter(e -> e.getValue() == task)
                  .map(Entry::getKey)
                  .findFirst();
         return id.orElse("worker");
      }
   }
}
