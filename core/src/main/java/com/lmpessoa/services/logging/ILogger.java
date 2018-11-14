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

import com.lmpessoa.services.services.IConfigurable;

/**
 * Enables logging messages throughout the application.
 *
 * <p>
 * Messages will be logged to the configured destination and, if applicable, using the configured
 * format. If no destination or format are defined, the application will use defaults (outputs to
 * the console).
 * </p>
 */
@NonTraced
public interface ILogger extends IConfigurable<ILoggerOptions> {

   /**
    * Returns a new instance of a logger.
    *
    * <p>
    * Applications should not call this method directly as it is meant for internal use only. To get a
    * logger from a resource, add an <code>ILogger</code> argument to its constructor.
    * </p>
    *
    * @return a new instance of a logger.
    */
   static ILogger newInstance() {
      return new Logger();
   }

   /**
    * Logs a fatal message.
    *
    * <p>
    * Fatal messages indicate a severe error that cause the application (or a thread) to terminate
    * prematurely.
    * </p>
    *
    * @param message the message to be logged.
    */
   void fatal(Object message);

   default void fatal(String message, Object... args) {
      fatal(String.format(message, args));
   }

   /**
    * Logs an error message.
    *
    * <p>
    * Error messages indicates an unexpected error or exception occurred but the application managed to
    * handle it and can continue. Part of the processing when this error occurred may be lost.
    * </p>
    *
    * @param message the message to be logged.
    */
   void error(Object message);

   default void error(String message, Object... args) {
      error(String.format(message, args));
   }

   /**
    * Logs a warning message.
    *
    * <p>
    * Warning messages indicate an unexpected situation (not mandatorily an exception) occurred and the
    * application handled it but the situation was worth registering.
    * </p>
    *
    * @param message the message to be logged.
    */
   void warning(Object message);

   default void warning(String message, Object... args) {
      warning(String.format(message, args));
   }

   /**
    * Logs an informative message.
    *
    * <p>
    * Information messages are used to register interesting runtime events (i.e. startup/shutdown).
    * Usually information messages do not have their source in exceptions.
    * </p>
    *
    * @param message the message to be logged.
    */
   void info(Object message);

   default void info(String message, Object... args) {
      info(String.format(message, args));
   }

   /**
    * Logs a debug message.
    *
    * <p>
    * Debug messages are used to register information about the application execution that can be used
    * to understand if the application behaviour is correct.
    * </p>
    *
    * @param message the message to be logged.
    */
   void debug(Object message);

   default void debug(String message, Object... args) {
      debug(String.format(message, args));
   }
}
