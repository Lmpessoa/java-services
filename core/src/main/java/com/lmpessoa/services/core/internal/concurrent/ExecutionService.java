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
package com.lmpessoa.services.core.internal.concurrent;

import java.lang.reflect.InvocationTargetException;
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

import com.lmpessoa.services.core.concurrent.IExecutionService;
import com.lmpessoa.services.core.internal.hosting.InternalServerError;
import com.lmpessoa.services.util.logging.ILogger;

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

   public boolean isTerminated() {
      return shutdown.get() && workers.isEmpty();
   }

   public void shutdown() {
      shutdown(false);
   }

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

   public String submit(Runnable task) {
      return submit(Executors.callable(task));
   }

   public String submit(Runnable task, String prefix) {
      return submit(Executors.callable(task), prefix);
   }

   public <T> String submit(Callable<T> task) {
      if (shutdown.get()) {
         throw new RejectedExecutionException();
      }
      Task<?> rtask = new Task<>(task);
      String id = store(rtask);
      executeOrQueue(rtask);
      return id;
   }

   public <T> String submit(Callable<T> task, String prefix) {
      if (shutdown.get()) {
         throw new RejectedExecutionException();
      }
      Task<?> rtask = new Task<>(task);
      String id = store(rtask, prefix);
      executeOrQueue(rtask);
      return id;
   }

   public long getKeepAliveTime(TimeUnit unit) {
      return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
   }

   public void setKeepAliveTimout(long timeout, TimeUnit unit) {
      if (timeout < 0) {
         throw new IllegalArgumentException();
      }
      keepAliveTime = unit.toNanos(timeout);
   }

   public long getResultRetentionTimeout(TimeUnit unit) {
      return unit.convert(resultRetentionTimeout, TimeUnit.NANOSECONDS);
   }

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
      private final transient Callable<T> job;
      private transient Worker worker = null;
      private transient Throwable exception = null;
      private transient Instant completed;

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
            return get(1, TimeUnit.MINUTES);
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
            } catch (Throwable t) {
               exception = t;
               while (exception instanceof InvocationTargetException
                        || exception instanceof InternalServerError) {
                  exception = exception.getCause();
               }
               status = Status.FAILED;
               error = t.getMessage();
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
