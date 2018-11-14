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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.lmpessoa.services.hosting.Application;
import com.lmpessoa.util.ClassUtils;
import com.lmpessoa.util.ConnectionInfo;

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
@NonTraced
public final class LogEntry {

   private final ZonedDateTime time;
   private final Severity severity;
   private final Object message;

   private final StackTraceElement logPoint;
   private final ConnectionInfo connInfo;
   private final String threadName;
   private final long threadId;

   private Throwable[] throwableStack = null;

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
      if (message instanceof Throwable) {
         Throwable[] stack = getThrowableStack();
         return stack[0].toString();
      } else {
         return message.toString();
      }
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
      if (message instanceof Throwable) {
         Throwable[] stack = getThrowableStack();
         return stack[0].getStackTrace()[0].getClassName();
      } else if (logPoint != null) {
         return logPoint.getClassName();
      }
      return Application.currentApp().getStartupClass().getName();
   }

   /**
    * Returns the information about the connection that made the current request.
    *
    * @return the connection information about the current request.
    */
   public ConnectionInfo getConnection() {
      return connInfo;
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

   @Override
   public String toString() {
      return String.format("[%s] %s: %s", getSeverity(), getClassName(), getMessage());
   }

   LogEntry(Severity severity, Object message, ConnectionInfo connInfo) {
      this(ZonedDateTime.now(), severity, message, connInfo);
   }

   LogEntry(ZonedDateTime time, Severity severity, Object message, ConnectionInfo connInfo) {
      this.time = time;
      this.severity = Objects.requireNonNull(severity);
      this.message = Objects.requireNonNull(message);
      this.connInfo = connInfo;

      Thread thread = Thread.currentThread();
      logPoint = getFirstNonTraced(thread.getStackTrace());
      threadName = thread.getName();
      threadId = thread.getId();
   }

   String[] getAdditionalMessages() {
      if (message instanceof Throwable) {
         Throwable[] stack = getThrowableStack();
         List<String> result = new ArrayList<>();
         for (StackTraceElement element : cleanNonTraced(stack[0].getStackTrace())) {
            result.add(String.format("...at %s", element.toString()));
         }
         return result.toArray(new String[0]);
      }
      return new String[0];
   }

   LogEntry getTraceEntry() {
      if (message instanceof Throwable) {
         Throwable[] stack = getThrowableStack();
         if (stack.length > 1) {
            return new LogEntry(this, Severity.TRACE, stack[1]);
         }
         return null;
      }
      if (this.severity != Severity.TRACE) {
         return new LogEntry(this, Severity.TRACE, "...at " + logPoint);
      }
      return null;
   }

   private LogEntry(LogEntry parentEntry, Severity severity, Object message) {
      this.time = parentEntry.getTime();
      this.severity = Objects.requireNonNull(severity);
      this.message = Objects.requireNonNull(message);
      this.logPoint = parentEntry.logPoint;
      this.threadName = parentEntry.threadName;
      this.threadId = parentEntry.threadId;
      this.connInfo = parentEntry.connInfo;
   }

   private static boolean skipNonTraced(StackTraceElement element) {
      if (element.isNativeMethod()) {
         return true;
      }
      String className = element.getClassName();
      if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("sun.")) {
         return true;
      }
      Class<?> clazz;
      try {
         clazz = Class.forName(className);
      } catch (ClassNotFoundException e) {
         return true;
      }
      if (clazz.isAnnotationPresent(NonTraced.class)) {
         return true;
      }
      final String methodName = element.getMethodName();
      if ("<cinit>".equals(methodName)) {
         return false;
      }
      Object[] methods;
      if ("<init>".equals(methodName)) {
         methods = ClassUtils.findConstructor(clazz, m -> !m.isSynthetic() && !m.isAnnotationPresent(NonTraced.class));
      } else {
         methods = ClassUtils.findMethods(clazz,
                  m -> m.getName().equals(methodName) && !m.isSynthetic() && !m.isAnnotationPresent(NonTraced.class));
      }
      return methods.length == 0;
   }

   private static StackTraceElement getFirstNonTraced(StackTraceElement[] stack) {
      for (StackTraceElement element : stack) {
         if (!skipNonTraced(element)) {
            return element;
         }
      }
      return null;
   }

   private static StackTraceElement[] cleanNonTraced(StackTraceElement[] stack) {
      return Arrays.stream(stack).filter(e -> !skipNonTraced(e)).toArray(StackTraceElement[]::new);
   }

   private Throwable[] getThrowableStack() {
      if (throwableStack == null) {
         List<Throwable> tlist = new ArrayList<>();
         Throwable t = (Throwable) message;
         tlist.add(t);
         while (t.getCause() != null && t != t.getCause()) {
            t = t.getCause();
            tlist.add(0, t);
         }
         throwableStack = tlist.toArray(new Throwable[0]);
      }
      return throwableStack;
   }
}
