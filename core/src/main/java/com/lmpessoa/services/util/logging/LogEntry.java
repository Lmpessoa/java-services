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

import java.time.ZonedDateTime;

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
public interface LogEntry {

   /**
    * Returns the date and time when the log entry was created.
    *
    * @return the date and time when the log entry was created.
    */
   ZonedDateTime getTime();

   /**
    * Returns the severity of the event of this log entry.
    *
    * <p>
    * The value of this method is equivalent to the method of the {@link ILogger} interface which
    * was called to create the log entry.
    * </p>
    *
    * @return the severity of the event of this log entry.
    */
   Severity getSeverity();

   /**
    * Returns the actual message of the log entry event.
    *
    * @return the actual message of the log entry event.
    */
   String getMessage();

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
   String getClassName();

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
   String getThreadName();

   /**
    * Returns the ID of the thread in which the log entry was created.
    *
    * @return the ID of the thread in which the log entry was created.
    */
   long getThreadId();

   StackTraceElement[] getStackTrace();

   /**
    * Returns the message of the log entry as an object.
    *
    * <p>
    * Log entries automatically convert any value registered with the log methods of the
    * {@link ILogger} interface to a string message. This method returns the original object sent
    * for registration with the logging subsystem.
    * </p>
    *
    * @return the message of the log entry as an object.
    */
   Object getMessageSource();
}
