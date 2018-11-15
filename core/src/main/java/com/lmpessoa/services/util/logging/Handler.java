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

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A Handler takes log messages from a Logger and exports them. It might for example, write them to
 * a console or to a file, or send them to a network logging service, or forward them to an OS log,
 * or whatever.
 * <p>
 * Because they are intended to help monitor and diagnose an application during operation, Handler
 * classes are configured only through the application settings file, including where log messages
 * are to be sent, which severities are to be handled and how they should be displayed. See the
 * specific documentation for each concrete Handler class for they contain information about which
 * settings are available and/or required for each.
 * </p>
 * <p>
 * Applications are not capable of discovering which Handlers are applied to a Logger. They are to
 * be concerned only with what and where to send messages for logging.
 * </p>
 * <p>
 * <b>Configuration:</b><br/>
 * Log handlers must be defined under the {@code log} section of the configuration file. Each entry
 * under this defines one handler. Each application can use as many handlers as it requires. If no
 * handler are defines, all log messages will be sent to the console.
 * </p>
 * <p>
 * The type of the handler must be provided on the {@code type} parameter. If this parameter is
 * omitted, the handler will assume to be the console.
 * </p>
 * <p>
 * Each handler must define which level of severity of log messages it will handle. These can be
 * defined with the properties {@code above} and {@code below} on the configuration file, inclusive
 * (the severity level indicated will also be included).
 * </p>
 * <p>
 * Handlers may also define different severity level per package of the source class, that is, you
 * can specify that different packages may be logged differently by the handler. In the handler
 * definition, packages must be listed under {@code packages} with {@code name} and their own
 * {@code above} and {@code below} settings.
 * </p>
 * <p>
 * The following configuration (in YAML format) defines that DEBUG messages must be sent to the
 * console while FATAL, ERROR and WARNING messages must be stored into a file, but messages sent
 * from the 'org.example.data' package are not to be logged to the file:
 * </p>
 *
 * <pre>
 * log:
 * - type: console
 *   below: debug
 * - type: file
 *   filename: /var/log/service.log
 *   above: warning
 *   packages:
 *   - name: org.example.data
 *     above: none
 * </pre>
 */
public abstract class Handler {

   private final Predicate<LogEntry> filter;

   /**
    * Creates a new {@code Handler} with the given filter.
    *
    * @param filter the filter used to test if messages should be handled by this Handler.
    */
   public Handler(Predicate<LogEntry> filter) {
      this.filter = Objects.requireNonNull(filter);
   }

   /**
    * Logged messages may be sent in batches to Handler classes. This method is called once before
    * the first entry of each batch is actually sent for logging. Subclasses may use this method to
    * perform any initialisation they require before logging messages, like allocating resources or
    * connecting to remote services.
    */
   protected void prepare() {
      // Must be overridden by subclasses
   }

   /**
    * Appends a message to the log destination. This method is called once for each log entry in a
    * batch. Subclasses must implement in this method any operations required to be done to export
    * the log entry to its destination.
    *
    * @param entry the log entry to be exported.
    */
   protected abstract void append(LogEntry entry);

   /**
    * Logged messages may be sent in batches to Handler classes. This method is called once after
    * the last entry of each batch has been appended by the handler. Subclasses may use this method
    * to perform any finalisation the require after logging messages, like releasing resources or
    * closing connection to remote services.
    */
   protected void finished() {
      // Must be overridden by subclasses
   }

   void consume(LogEntry entry) {
      if (filter.test(entry)) {
         append(entry);
      }
   }
}
