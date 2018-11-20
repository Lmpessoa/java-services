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

import java.lang.reflect.Array;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a log entry.
 *
 * <p>
 * A <code>LogEntry</code> object is the sum of raw information required to create a log entry in
 * the underlying storage medium.
 * </p>
 *
 * <p>
 * Instances of this class cannot be seldom created; it is provided automatically by the engine
 * whenever one of the methods of the {@link ILogger} interface is called.
 * </p>
 */
public final class LogEntry {

   private static final String AT = "\n...at ";

   private final Map<Class<?>, Object> extraInfo;
   private final StackTraceElement[] logPoint;
   private final ZonedDateTime time;
   private final Severity severity;
   private final String threadName;
   private final Object message;
   private final Logger log;
   private final long threadId;

   /**
    * Returns the date and time when the log entry was created.
    *
    * @return the date and time when the log entry was created.
    */
   public ZonedDateTime getTime() {
      return time;
   }

   /**
    * Returns the severity of the event of this log entry.
    *
    * <p>
    * The value of this method is equivalent to the method of the {@link ILogger} interface which was
    * called to create the log entry.
    * </p>
    *
    * @return the severity of the event of this log entry.
    */
   public Severity getSeverity() {
      return severity;
   }

   /**
    * Returns the actual message of the log entry event.
    *
    * @return the actual message of the log entry event.
    */
   public String getMessage() {
      return getMessageResult(message).trim();
   }

   /**
    * Returns the name of the class in which the log entry was created.
    *
    * <p>
    * This is exactly the name of the class in which one of the methods of the {@link ILogger}
    * interface was called to create this log instance.
    * </p>
    *
    * @return the name of the class in which the log entry was created.
    */
   public String getClassName() {
      return log.getClassName(logPoint);
   }

   /**
    * Returns the name of the thread in which the log entry was created.
    *
    * <p>
    * Please note that thread names are not unique in the system thus multiple threads can bear the
    * same name at the same time.
    * </p>
    *
    * @return the name of the thread in which the log entry was created.
    */
   public String getThreadName() {
      return threadName;
   }

   /**
    * Returns the ID of the thread in which the log entry was created.
    *
    * @return the ID of the thread in which the log entry was created.
    */
   public long getThreadId() {
      return threadId;
   }

   public StackTraceElement[] getStackTrace() {
      return log.filterStack(logPoint);
   }

   /**
    * Returns the message of the log entry as an object.
    *
    * <p>
    * Log entries automatically convert any value registered with the log methods of the
    * {@link ILogger} interface to a string message. This method returns the original object sent for
    * registration with the logging subsystem.
    * </p>
    *
    * @return the message of the log entry as an object.
    */
   public Object getMessageSource() {
      return message;
   }

   @Override
   public String toString() {
      return String.format("[%s] %s: %s", getSeverity(), getClassName(), getMessage());
   }

   LogEntry(Severity severity, Object message, Map<Class<?>, Object> extraInfo, Logger log) {
      this(ZonedDateTime.now(), severity, message, extraInfo, log);
   }

   LogEntry(ZonedDateTime time, Severity severity, Object message, Map<Class<?>, Object> extraInfo, Logger log) {
      Thread thread = Thread.currentThread();
      this.logPoint = message instanceof Throwable ? ((Throwable) message).getStackTrace() : thread.getStackTrace();
      this.extraInfo = extraInfo != null ? extraInfo : Collections.emptyMap();
      this.severity = Objects.requireNonNull(severity);
      this.message = Objects.requireNonNull(message);
      this.log = Objects.requireNonNull(log);
      this.threadName = thread.getName();
      this.threadId = thread.getId();
      this.time = time;
   }

   Logger getLogger() {
      return log;
   }

   @SuppressWarnings("unchecked")
   <T> T getExtra(Class<T> type) {
      return (T) extraInfo.get(type);
   }

   private String getMessageResult(Object message) {
      if (message.getClass().isArray()) {
         Collection<Object> tmp = new ArrayList<>();
         int len = Array.getLength(message);
         for (int i = 0; i < len; ++i) {
            tmp.add(Array.get(message, i));
         }
         message = tmp;
      }
      StringBuilder result = new StringBuilder();
      if (message instanceof Collection) {
         for (Object o : (Collection<?>) message) {
            String s = getMessageResult(o);
            if (!s.isEmpty()) {
               result.append(s);
               result.append("\n");
            }
         }
      } else {
         result.append(message.toString());
      }
      if (message instanceof Throwable || isTracing()) {
         StackTraceElement[] filteredStack = getStackTrace();
         for (StackTraceElement item : filteredStack) {
            result.append(AT + item);
         }
      }
      if (message instanceof Throwable && isTracing()) {
         Throwable cause;
         while ((cause = ((Throwable) message).getCause()) != null) {
            result.append('\n');
            result.append(getMessageResult(cause));
         }
      }
      return result.toString();
   }

   private boolean isTracing() {
      return log != null && log.isTracing();
   }
}
