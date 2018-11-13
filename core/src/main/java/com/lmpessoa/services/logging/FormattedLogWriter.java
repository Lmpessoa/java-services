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
 * Sublasses of this writer should output the result of calling {@link #format(LogEntry)} instead of
 * providing their own to enable the format to be specified by developers.
 * </p>
 */
public abstract class FormattedLogWriter implements LogWriter {

   private LogFormatter formatter = null;

   /**
    * Formats the given log entry according to the defined format.
    * 
    * @param entry the log entry to be formatted.
    * @return the formatted log entry.
    */
   protected final String format(LogEntry entry) {
      return formatter.format(entry);
   }

   void setFormatter(LogFormatter formatter) {
      this.formatter = formatter;
   }
}
