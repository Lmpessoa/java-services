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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.lmpessoa.services.core.hosting.ConnectionInfo;

/**
 * A Logger is used to register information about the execution of an application.
 * <p>
 * Messages sent to be logged my be of any type of object (beware they provide meaningful
 * information from their {@link #toString()} method) or a string to be formatted using any number
 * of variables (messages to be formatted behave exactly like
 * {@link String#format(String, Object...)}).
 * </p>
 * <p>
 * In order to decide what to do with the message to be logger, all Loggers use a {@link Handler}
 * which implement the effective operation to store the logged message. By default all messages are
 * sent to the console but it is also possible to send messages to a file or to a remote service
 * using a different {@code Handler}.
 * </p>
 * <p>
 * Loggers can be further configured to ignore certain messages based on the level of the message
 * (meaning that only messages on that level or above are important to the operation of the
 * application) and further by level and package of the source of the message (meaning for messages
 * originated from that specific package another level of messages are important).
 * </p>
 */
public final class Logger implements ILogger {

   private final Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();
   private final Map<String, VariableInfo> variables = new HashMap<>();

   private final Queue<LogEntry> entriesToLog = new LinkedBlockingQueue<>();
   private final Predicate<StackTraceElement> stackFilter;
   private final Set<Handler> handlers = new HashSet<>();
   private final Class<?> defaultClass;

   private boolean tracing = false;
   private Thread thread = null;

   /**
    * Creates a new default {@code Logger}.
    */
   public Logger() {
      this(getCallerClass(), Logger::defaultStackFilter);
   }

   /**
    * Creates a new default {@code Logger} with the given initial handler.
    *
    * @param firstHandler the first handler of this {@code Logger}.
    */
   public Logger(Handler firstHandler) {
      this(getCallerClass(), Logger::defaultStackFilter);
      this.handlers.add(firstHandler);
   }

   /**
    * Create a new {@code Logger} with the given arguments.
    *
    * @param defaultClass the class to be used as default class.
    * @param stackFilter a filter to reduce the amount of {@link StackTracElement}s in logged messages.
    * @param connectionSupplier a supplier of {@link ConnectionInfo} for the {@code Logger}.
    */
   public Logger(Class<?> defaultClass, Predicate<StackTraceElement> stackFilter) {
      this.defaultClass = Objects.requireNonNull(defaultClass);
      this.stackFilter = Objects.requireNonNull(stackFilter);
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
    * Adds the given {@link Handler} to this {@code Logger}.
    * <p>
    * Since it makes no sense for the same handler to be added multiple times to a logger, if the given
    * handler is already assigned to this, this method will fail silently.
    * </p>
    *
    * @param handler the handler to be added to this logger.
    */
   public void addHandler(Handler handler) {
      handlers.add(handler);
   }

   /**
    * Removes the given {@link Handler} from this {@code Logger}.
    * <p>
    * This method will fail silently if the given {@code Handler} is not assigned to this logger.
    * </p>
    *
    * @param handler the handler to be removed from this logger.
    */
   public void removeHandler(Handler handler) {
      handlers.remove(handler);
   }

   public <T> void addSupplier(Class<T> type, Supplier<T> supplier) {
      if (suppliers.containsKey(Objects.requireNonNull(type))) {
         throw new IllegalArgumentException("Supplier for type is already defined: " + type.getName());
      }
      suppliers.put(type, Objects.requireNonNull(supplier));
   }

   public <T> void addVariable(String name, Class<T> type, Function<T, String> func) {
      if (variables.containsKey(name)) {
         throw new IllegalArgumentException("Variable is already defined: " + name);
      }
      variables.put(name, new VariableInfo(Objects.requireNonNull(type), //
               Objects.requireNonNull(func)));
   }

   /**
    * Sets whether this Logger should enable further tracing.
    * <p>
    * When tracing is enabled, additional details from logged messages will be logged. Specifically,
    * logged exceptions will register all causes for the exception instead of just the original
    * exception, and any other message types will register the method where the log entry was called.
    * </p>
    *
    * @param tracing {@code true} if this logger should register additional information for each
    * entrty, {@code false} otherwise.
    */
   public void enableTracing(boolean tracing) {
      this.tracing = tracing;
   }

   /**
    * Returns whether messages sent to this logger are being traced.
    * <p>
    * When tracing is enabled, additional details from logged messages will be logged. Specifically,
    * logged exceptions will register all causes for the exception instead of just the original
    * exception, and any other message types will register the method where the log entry was called.
    * </p>
    *
    * @return {@code true} if messages sent to this logger are being traced, {@code false} otherwise.
    */
   public boolean isTracing() {
      return tracing;
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

   static boolean defaultStackFilter(StackTraceElement trace) {
      if (trace.isNativeMethod() || Thread.class.getName().equals(trace.getClassName())
               && "getStackTrace".equals(trace.getMethodName())) {
         return false;
      }
      List<String> classes = Arrays.asList(new String[] { //
               ILogger.class.getName(), Logger.class.getName(), LogEntry.class.getName() });
      return !classes.contains(trace.getClassName());
   }

   StackTraceElement[] filterStack(StackTraceElement[] trace) {
      return Arrays.stream(trace).filter(stackFilter).toArray(StackTraceElement[]::new);
   }

   String getClassName(StackTraceElement[] trace) {
      trace = filterStack(trace);
      if (trace.length > 0) {
         return trace[0].getClassName();
      }
      return defaultClass.getName();
   }

   Function<LogEntry, String> getVariable(String name) {
      return variables.get(name);
   }

   private static Class<?> getCallerClass() {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      try {
         return Class.forName(trace[3].getClassName());
      } catch (ClassNotFoundException e) {
         // Should never enter here since class exists
         throw new RuntimeException(e); // NOSONAR
      }
   }

   private void log(Severity level, Object message) {
      Map<Class<?>, Object> extraInfo = new HashMap<>();
      for (Entry<Class<?>, Supplier<?>> entry : suppliers.entrySet()) {
         extraInfo.put(entry.getKey(), entry.getValue().get());
      }
      LogEntry entry = new LogEntry(level, message, extraInfo, this);
      entriesToLog.add(entry);
      runLoggerJob();
   }

   private synchronized void runLoggerJob() {
      if (thread == null || !thread.isAlive()) {
         thread = new Thread((Runnable) () -> {
            try {
               handlers.stream().forEach(Handler::prepare);
               while (!entriesToLog.isEmpty()) {
                  LogEntry entry = entriesToLog.poll();
                  handlers.parallelStream().forEach(handler -> handler.consume(entry));
               }
            } catch (Throwable e) { // NOSONAR
               // Resort to default behaviour since trying to log the thrown exception with
               // our own engine could cause a loop in exception causing
               e.printStackTrace(); // NOSONAR
            } finally {
               handlers.stream().forEach(Handler::finished);
               thread = null;
            }
         }, "logger");
         thread.start();
      }
   }

   private static class VariableInfo implements Function<LogEntry, String> {

      private final Function<Object, String> func;
      private final Class<?> type;

      @SuppressWarnings("unchecked")
      <T extends Object> VariableInfo(Class<T> type, Function<T, String> func) {
         this.func = (Function<Object, String>) func;
         this.type = type;
      }

      @Override
      public String apply(LogEntry entry) {
         Object obj = entry.getExtra(type);
         return func.apply(obj);
      }
   }
}
