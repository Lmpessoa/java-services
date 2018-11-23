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
package com.lmpessoa.services.util.logging.internal;

import java.lang.reflect.Array;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.lmpessoa.services.util.logging.LogEntry;
import com.lmpessoa.services.util.logging.Severity;

final class LogEntryImpl implements LogEntry {

   private static final String AT = "\n...at ";

   private final Map<Class<?>, Object> extraInfo;
   private final StackTraceElement[] logPoint;
   private final ZonedDateTime time;
   private final Severity severity;
   private final String threadName;
   private final Object message;
   private final Logger log;
   private final long threadId;

   @Override
   public ZonedDateTime getTime() {
      return time;
   }

   @Override
   public Severity getSeverity() {
      return severity;
   }

   @Override
   public String getMessage() {
      return getMessageResult(message).trim();
   }

   @Override
   public String getClassName() {
      StackTraceElement[] trace = log.filterStack(logPoint);
      if (trace.length > 0) {
         return trace[0].getClassName();
      }
      return log.getDefaultClass().getName();
   }

   @Override
   public String getThreadName() {
      return threadName;
   }

   @Override
   public long getThreadId() {
      return threadId;
   }

   @Override
   public StackTraceElement[] getStackTrace() {
      return log.filterStack(logPoint);
   }

   @Override
   public Object getMessageSource() {
      return message;
   }

   public Logger getLogger() {
      return log;
   }

   @Override
   public String toString() {
      return String.format("[%s] %s: %s", getSeverity(), getClassName(), getMessage());
   }

   LogEntryImpl(Severity severity, Object message, Map<Class<?>, Object> extraInfo, Logger log) {
      this(ZonedDateTime.now(), severity, message, extraInfo, log);
   }

   LogEntryImpl(ZonedDateTime time, Severity severity, Object message,
      Map<Class<?>, Object> extraInfo, Logger log) {
      Thread thread = Thread.currentThread();
      this.logPoint = message instanceof Throwable ? ((Throwable) message).getStackTrace()
               : thread.getStackTrace();
      this.extraInfo = extraInfo != null ? extraInfo : Collections.emptyMap();
      this.severity = Objects.requireNonNull(severity);
      this.message = Objects.requireNonNull(message);
      this.log = Objects.requireNonNull(log);
      this.threadName = thread.getName();
      this.threadId = thread.getId();
      this.time = time;
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
