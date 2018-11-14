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
package com.lmpessoa.util;

import java.text.ParseException;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.logging.ILoggerOptions;
import com.lmpessoa.services.logging.LogEntry;
import com.lmpessoa.services.logging.LogWriter;
import com.lmpessoa.services.logging.Severity;

final class NullLogger implements ILogger {

   @Override
   public void fatal(Object message) {
      // Test method, does nothing
   }

   @Override
   public void error(Object message) {
      // Test method, does nothing
   }

   @Override
   public void warning(Object message) {
      // Test method, does nothing
   }

   @Override
   public void info(Object message) {
      // Test method, does nothing
   }

   @Override
   public void debug(Object message) {
      // Test method, does nothing
   }

   @Override
   public void setConnectionSupplier(Supplier<ConnectionInfo> supplier) {
      // Test method, does nothing
   }

   @Override
   public ILoggerOptions getOptions() {
      return new ILoggerOptions() {

         @Override
         public void useWriter(LogWriter writer) {
            // Test method, does nothing
         }

         @Override
         public void useTemplate(String template) throws ParseException {
            // Test method, does nothing
         }

         @Override
         public void setPackageLevel(String packageName, Severity level) {
            // Test method, does nothing
         }

         @Override
         public void setDefaultLevel(Severity level) {
            // Test method, does nothing
         }

         @Override
         public void addVariable(String label, Function<LogEntry, String> func) {
            // Test method, does nothing
         }
      };
   }
}
