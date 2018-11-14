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

import com.lmpessoa.services.util.logging.LogEntry;
import com.lmpessoa.services.util.logging.LogWriter;
import com.lmpessoa.services.util.logging.Severity;

final class TestLogWriter extends LogWriter {

   private LogEntry entry;

   @Override
   public void append(LogEntry entry) {
      this.entry = entry;
   }

   String[] getLastAdditionalMessages() {
      return entry != null ? entry.getAdditionalMessages() : new String[0];
   }

   LogEntry getLastTrace() {
      return entry != null ? entry.getTraceEntry() : null;
   }

   String getLastMessage() {
      return entry != null ? entry.getMessage() : null;
   }

   Severity getLastSeverity() {
      return entry != null ? entry.getSeverity() : null;
   }
}
