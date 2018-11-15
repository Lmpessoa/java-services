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

import com.lmpessoa.services.util.ConnectionInfo;

/**
 * A Logger is used to register information about the execution of an application.
 *
 * <p>
 * Messages sent to be logged my be of any type of object (beware they provide meaningful
 * information from their {@link #toString()} method) or a string to be formatted using any number
 * of variables (messages to be formatted behave exactly like
 * {@link String#format(String, Object...)}).
 * </p>
 *
 * <p>
 * In order to decide what to do with the message to be logger, all Loggers use a {@link LogWriter}
 * which implement the effective operation to store the logged message. By default all messages are
 * sent to the console but it is also possible to send messages to a file or to a remote service
 * using a different {@code LogWriter}.
 * </p>
 *
 * <p>
 * Loggers can be further configured to ignore certain messages based on the level of the message
 * (meaning that only messages on that level or above are important to the operation of the
 * application) and further by level and package of the source of the message (meaning for messages
 * originated from that specific package another level of messages are important).
 * </p>
 */
public final class Logger implements ILogger {

   private final Queue<LogEntry> entriesToLog = new ArrayDeque<>();

   private final Class<?> defaultClass;
   private final LogWriter writer;

   private Supplier<ConnectionInfo> connSupplier;
   private boolean tracing = false;
   private Thread thread = null;

   private Map<String, Severity> packageLevels = new HashMap<>();
   private Severity defaultLevel = Severity.INFO;

   /**
    * Creates a new {@code Logger} using the default output.
    *
    * @param defaultClass the class that should be associated with log entries when the source of the
    * message cannot be determined.
    */
   public Logger(Class<?> defaultClass) {
      this(defaultClass, new ConsoleLogWriter());
   }

   /**
    * Creates a new {@code Logger} using the given log writer.
    *
    * @param defaultClass the class that should be associated with log entries when the source of the
    * message cannot be determined.
    * @param writer a writer that defines what to do with logged messages.
    */
   public Logger(Class<?> defaultClass, LogWriter writer) {
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

   /**
    * Sets the default log level for this {@code Logger}.
    *
    * <p>
    * After calling this method, the Logger will ignore any messages whose level is below that of the
    * argument of this method. For example, if this method is called with {@link Severity#WARNING}, any
    * messages logged using {@code info(...)} or {@code debug(...)} will not be registered by the
    * Logger.
    * </p>
    *
    * @param level the default log level for this Logger.
    */
   public void setDefaultLevel(Severity level) {
      this.defaultLevel = Objects.requireNonNull(level);
   }

   /**
    * Sets a specific log level for classes of the given package.
    *
    * <p>
    * The behaviour of this method is similar to {@link #setDefaultLevel(Severity)} except that it
    * applies only to classes of the given package. After a call to this method, messages to be logged
    * coming from classes in the given package will be filtered using the specified severity as filter
    * while all other classes will still be subject to the default level.
    * </p>
    *
    * <p>
    * This method can be called several times with as many different packages as desired. Calling this
    * method a second time for a previously registered package will change that the log level for that
    * package only. Calling this method with a null level will reset log level for the package will
    * reset it to use the default log level.
    * </p>
    *
    * @param packageName
    * @param level
    */
   public void setPackageLevel(String packageName, Severity level) {
      Objects.requireNonNull(packageName);
      if (packageName.isEmpty() || packageName.matches("\\s")) {
         throw new IllegalArgumentException("Not a valid package name: " + packageName);
      }
      if (packageLevels.containsKey(packageName) && level == null) {
         packageLevels.remove(packageName);
      } else if (level != null) {
         packageLevels.put(packageName, level);
      }
   }

   /**
    * Sets the a supplier of {@link ConnectionInfo} to be used with this logger.
    *
    * <p>
    * Logged messages may be comprised of information from the connection/request the message is
    * associated with. But in order to obtain this information it is required that the Logger has means
    * to obtain this information.
    * </p>
    *
    * <p>
    * If the logger has no supplier for {@code ConnectionInfo}, then no information about a connection
    * will be available.
    * </p>
    *
    * @param supplier
    */
   public void setConnectionSupplier(Supplier<ConnectionInfo> supplier) {
      this.connSupplier = supplier;
   }

   /**
    * Sets whether this Logger should enable further tracing.
    *
    * <p>
    * When tracing is enabled, additional details from logged messages will be logged. Specifically,
    * logged exceptions will register all causes for the exception instead of just the original
    * exception, and any other message types will register the method where the log entry was called.
    * </p>
    *
    * @param tracing {@code true} if this logger should register additional information for each
    * entry, {@code false} otherwise.
    */
   public void enableTracing(boolean tracing) {
      this.tracing = tracing;
   }

   /**
    * Waits for the logger task to finish handling the message queue.
    *
    * <p>
    * Log messages are not handled by the main thread to prevent a long operation from blocking the
    * application response due to a request to log a message. This method will wait for the internal
    * message queue of the logger to be emptied before returning.
    * </p>
    *
    * @throws InterruptedException if the thread calling this method was interrupted by another thread.
    * The interrupted status of the current thread is cleared when this exception is thrown.
    */
   public void join() throws InterruptedException {
      while (thread != null) {
         Thread.sleep(100);
      }
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
         thread = new Thread((Runnable) () -> {
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
         }, "logger");
         thread.start();
      }
   }
}
