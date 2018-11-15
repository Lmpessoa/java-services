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
package com.lmpessoa.services.core.hosting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.lmpessoa.services.util.logging.ILogger;

final class AsyncJobQueue {

   private final Map<String, Future<?>> callbacks = new HashMap<>();
   private final ExecutorService threadPool;
   private final ILogger log;

   AsyncJobQueue(ApplicationServerInfo info, ILogger log) {
      this.log = log;
      ThreadFactory factory = r -> {
         Thread result = new Thread(r);
         result.setUncaughtExceptionHandler((t, e) -> log.fatal(e));
         return result;
      };
      int maxJobCount = info.getIntProperty("requests.limit").orElse(0);
      if (maxJobCount > 0) {
         this.threadPool = Executors.newFixedThreadPool(maxJobCount, factory);
      } else {
         this.threadPool = Executors.newCachedThreadPool(factory);
      }
   }

   public synchronized String submit(Callable<?> job) {
      String key;
      do {
         key = UUID.randomUUID().toString();
      } while (callbacks.containsKey(key));
      Future<?> result = threadPool.submit(job);
      callbacks.put(key, result);
      return key;
   }

   public synchronized String submit(Runnable job) {
      String key;
      do {
         key = UUID.randomUUID().toString();
      } while (callbacks.containsKey(key));
      Future<?> result = threadPool.submit(job);
      callbacks.put(key, result);
      return key;
   }

   public void execute(Runnable job) {
      threadPool.execute(job);
   }

   public Future<?> getResult(String key) {
      return callbacks.get(key);
   }

   public void shutdown() {
      threadPool.shutdown();
      try {
         while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
            // Just sit and wait
         }
      } catch (InterruptedException e) { // NOSONAR
         log.error(e);
      }
   }
}
