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
package com.lmpessoa.services.util.logging;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Supplier;

import com.lmpessoa.services.Internal;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.ConnectionInfo;

@Internal
public final class Logger implements ILogger, Runnable {

   private final Queue<LogEntry> entriesToLog = new ArrayDeque<>();

   private final Class<?> defaultClass;
   private final LogWriter writer;

   private Supplier<ConnectionInfo> connSupplier;
   private boolean tracing = false;
   private Thread thread = null;

   private Map<String, Severity> packageLevels = new HashMap<>();
   private Severity defaultLevel = Severity.INFO;

   public Logger(Class<?> defaultClass) {
      this(defaultClass, new ConsoleLogWriter());
      ClassUtils.checkInternalAccess();
   }

   public Logger(Class<?> defaultClass, LogWriter writer) {
      ClassUtils.checkInternalAccess();
      this.defaultClass = defaultClass;
      this.writer = writer;
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

   @Override
   public void run() {
      writer.prepare();
      while (!entriesToLog.isEmpty()) {
         LogEntry entry;
         synchronized (entriesToLog) {
            entry = entriesToLog.poll();
         }
         while (entry != null) {
            writer.append(entry);
            entry = tracing ? entry.getTraceEntry() : null;
         }
      }
      writer.finished();
      thread = null;
   }

   @Override
   public void join() {
      ClassUtils.checkInternalAccess();
      if (thread != null) {
         try {
            thread.join();
         } catch (InterruptedException e) {
            thread.interrupt();
            // Just ignore
         }
      }
   }

   public void setDefaultLevel(Severity level) {
      ClassUtils.checkInternalAccess();
      this.defaultLevel = Objects.requireNonNull(level);
   }

   public void setPackageLevel(String packageName, Severity level) {
      ClassUtils.checkInternalAccess();
      Objects.requireNonNull(packageName);
      if (packageName.isEmpty() || packageName.matches("\\s")) {
         throw new IllegalArgumentException("Not a valid package name: " + packageName);
      }
      packageLevels.put(packageName, level);
   }

   public void setConnectionSupplier(Supplier<ConnectionInfo> supplier) {
      ClassUtils.checkInternalAccess();
      this.connSupplier = supplier;
   }

   public void enableTracing(boolean tracing) {
      ClassUtils.checkInternalAccess();
      this.tracing = tracing;
   }

   private void log(Severity level, Object message) {
      ConnectionInfo connInfo = connSupplier != null ? connSupplier.get() : null;
      LogEntry entry = new LogEntry(level, message, connInfo, defaultClass);
      if (shouldLog(entry)) {
         synchronized (entriesToLog) {
            entriesToLog.add(entry);
         }
         runLoggerJob();
      }
   }

   private boolean shouldLog(LogEntry entry) {
      String pckg = entry.getClassName();
      if (pckg != null) {
         while (pckg.lastIndexOf('.') > 0) {
            pckg = pckg.substring(0, pckg.lastIndexOf('.'));
            if (packageLevels.containsKey(pckg)) {
               Severity pckgLevel = packageLevels.get(pckg);
               return entry.getSeverity().compareTo(pckgLevel) <= 0;
            }
         }
      }
      return entry.getSeverity().compareTo(defaultLevel) <= 0;
   }

   private synchronized void runLoggerJob() {
      if (thread == null || !thread.isAlive()) {
         thread = new Thread(this, "logger");
         thread.start();
      }
   }
}
