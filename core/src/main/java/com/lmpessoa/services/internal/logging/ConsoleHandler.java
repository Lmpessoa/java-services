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
package com.lmpessoa.services.internal.logging;

import java.util.function.Predicate;

import com.lmpessoa.services.logging.FormattedHandler;
import com.lmpessoa.services.logging.LogEntry;
import com.lmpessoa.services.logging.Severity;

/**
 * A Handler that sends log messages to the system console.
 * <p>
 * By default each ConsoleHandler will send log messages of level {@link Severity#ERROR} and
 * {@Severity#FATAL} to the standard error output ({@link System#err}) and the remaining severity
 * messages to the standard output ({@link System#out}.
 * </p>
 * <p>
 * <b>Configuration:</b><br/>
 * Since a {@code ConsoleHandler} is a {@link FormattedHandler}, a different template can be defined
 * in the application settings file under the parameter {@code template}. A template will recognise
 * values in the format {@code ${VAR}} as variables to be replaced with information from the log
 * message entry.
 * </p>
 * <p>
 * It is also possible to define the severity level of messages to be highlighted using the
 * {@code highlight} parameter. Highlighted messages will be sent to {@code System.err} instead of
 * {@code System.out}.
 * </p>
 * <p>
 * In the following example, a logger is defined to send to the console only a timestamp, severity
 * and message text, highlighting messages of severity {@code WARNING} or above:
 * </p>
 *
 * <pre>
 * log:
 * - type: console
 *   highlight: warning
 *   template: ${Time} [${Severity}] ${Message}
 * </pre>
 */
public final class ConsoleHandler extends FormattedHandler {

   private Severity highlight = Severity.ERROR;

   /**
    * Creates a new {@code ConsoleHandler} with the given filter
    *
    * @param filter the filter used to test if messages should be handled by this Handler.
    */
   public ConsoleHandler(Predicate<LogEntry> filter) {
      super(filter);
   }

   /**
    * Sets the minimum severity level for highlighted messages.
    * <p>
    * Highlighted messages are sent to the standard error output ({@link System#err}) instead of the
    * standard output ({@code System#out}). Actual highlighting depends on configuration of the
    * underlying operating system.
    * </p>
    *
    * @param severity the minimum severity level for highlighted messages.
    */
   public void setHighlight(Severity severity) {
      this.highlight = severity;
   }

   @Override
   protected void append(String entry) {
      if (highlight.compareTo(currentEntry().getSeverity()) >= 0) {
         System.err.println(entry); // NOSONAR
      } else {
         System.out.println(entry); // NOSONAR
      }
   }
}
