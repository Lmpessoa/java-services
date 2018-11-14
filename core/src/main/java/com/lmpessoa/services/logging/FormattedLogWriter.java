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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A log writer which outputs formatted log entries.
 *
 * <p>
 * Instances of this class are used when log entries are output in pure text format and provide the
 * means to format log entries as defined by the developer during the configuration of the
 * application (or the default format if none was defined).
 * </p>
 *
 * <p>
 * Subclasses of this writer should output the result of calling {@link #format(LogEntry)} instead of
 * providing their own to enable the format to be specified by developers.
 * </p>
 */
public abstract class FormattedLogWriter implements LogWriter {

   public static final String REMOTED = "{Time}/{Local.Host:<12} {Severity:>7} -- [{Remote.Host:<15}] {Class.Name:<36} : {Message}";
   public static final String DEFAULT = "{Time} {Severity:>7} -- [{Remote.Host:<15}] {Class.Name:<36} : {Message}";

   private LogFormatter formatter = null;
   private boolean tracing = false;

   @Override
   public final void append(LogEntry entry) {
      append(entry.getSeverity(), format(entry));
   }

   protected abstract void append(Severity severity, String entry);

   void setFormatter(LogFormatter formatter) {
      this.formatter = formatter;
   }

   void setTracing(boolean tracing) {
      this.tracing = tracing;
   }

   /**
    * Formats the given log entry according to the defined format.
    *
    * @param entry the log entry to be formatted.
    * @return the formatted log entry.
    */
   private String format(LogEntry entry) {
      StringWriter buffer = new StringWriter();
      PrintWriter result = new PrintWriter(buffer);
      LogEntry n = entry;
      while (n != null) {
         result.println(formatter.format(n));
         for (String message : n.getAdditionalMessages()) {
            result.println(formatter.format(n, message));
         }
         n = tracing ? n.getTraceEntry() : null;
      }
      return buffer.toString();
   }
}
