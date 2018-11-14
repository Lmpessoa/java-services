/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.services.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.function.Supplier;

import com.lmpessoa.services.Internal;
import com.lmpessoa.util.ClassUtils;
import com.lmpessoa.util.ConnectionInfo;

@NonTraced
final class Logger implements ILogger, Runnable {

   private final Queue<LogEntry> entriesToLog = new ArrayDeque<>();
   private final LoggerOptions options = new LoggerOptions(this);

   private Collection<LogEntry> cachedEntries = new ArrayList<>();
   private Supplier<ConnectionInfo> connSupplier;
   private LogWriter writer = null;
   private Thread thread = null;

   @Internal
   Logger() {
      ClassUtils.checkInternalAccess();
   }

   @Override
   public void fatal(Object message) {
      log(Severity.FATAL, message);
   }

   @Override
   public void error(Object message) {
      log(Severity.ERROR, message);
   }

   @Override
   public void warning(Object message) {
      log(Severity.WARNING, message);
   }

   @Override
   public void info(Object message) {
      log(Severity.INFO, message);
   }

   @Override
   public void debug(Object message) {
      log(Severity.DEBUG, message);
   }

   @Internal
   @Override
   public ILoggerOptions getOptions() {
      ClassUtils.checkInternalAccess();
      return options;
   }

   @Override
   public void setConnectionSupplier(Supplier<ConnectionInfo> supplier) {
      this.connSupplier = supplier;
   }

   @Override
   public void run() {
      try {
         writer.prepare();
         while (true) {
            LogEntry entry;
            synchronized (entriesToLog) {
               entry = entriesToLog.poll();
            }
            if (entry == null) {
               return;
            }
            if (options.shouldLog(entry)) {
               writer.append(entry);
            }
         }
      } finally {
         writer.finish();
         thread = null;
      }
   }

   synchronized void configurationEnded() {
      writer = options.getWriter();
      entriesToLog.addAll(cachedEntries);
      cachedEntries = null;
      runQueue();
   }

   private synchronized void log(Severity level, Object message) {
      ConnectionInfo connInfo = connSupplier != null ? connSupplier.get() : null;
      LogEntry entry = new LogEntry(level, message, connInfo);
      if (!options.isConfigured()) {
         synchronized (cachedEntries) {
            cachedEntries.add(entry);
         }
      } else {
         synchronized (entriesToLog) {
            entriesToLog.add(entry);
         }
         runQueue();
      }
   }

   private void runQueue() {
      synchronized (this) {
         if (thread == null) {
            thread = new Thread(this, "logger");
            thread.start();
         }
      }
   }
}
