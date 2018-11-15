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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;

public abstract class FormattedLogWriter extends LogWriter {

   static final String DEFAULT = "{Time} {Severity:>7} -- [{Remote.Host:<15}] {Class.Name:<36} : {Message}";

   private LogFormatter template;

   public FormattedLogWriter() {
      try {
         this.template = LogFormatter.parse(DEFAULT);
      } catch (ParseException e) {
         // If it falls here, something is wrong with our code
         e.printStackTrace();
         System.exit(1);
      }
   }

   public void setTemplate(String template) throws ParseException {
      this.template = LogFormatter.parse(template);
   }

   @Override
   protected final void append(LogEntry entry) {
      append(entry.getSeverity(), format(entry));
   }

   protected abstract void append(Severity severity, String entry);

   private String format(LogEntry entry) {
      StringWriter buffer = new StringWriter();
      PrintWriter result = new PrintWriter(buffer);
      result.println(template.format(entry));
      for (String message : entry.getAdditionalMessages()) {
         result.println(template.format(entry, message));
      }
      return buffer.toString();
   }
}
