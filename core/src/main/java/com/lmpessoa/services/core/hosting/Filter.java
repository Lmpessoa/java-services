/*
 * Copyright (c) 2018 Leonardo Pessoa
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
package com.lmpessoa.services.core.hosting;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.lmpessoa.services.util.logging.LogEntry;
import com.lmpessoa.services.util.logging.Severity;

final class Filter implements Predicate<LogEntry> {

   private final Map<String, Predicate<Severity>> packages = new HashMap<>();
   private final Predicate<Severity> defaultLevel;

   Filter(Map<String, String> properties) {
      Severity above = Severity.valueOf(properties.getOrDefault("above", "DEBUG"));
      Severity below = Severity.valueOf(properties.getOrDefault("below", "FATAL"));
      defaultLevel = level -> above.compareTo(level) <= 0 && below.compareTo(level) >= 0;
   }

   @Override
   public final boolean test(LogEntry entry) {
      Predicate<Severity> result = packages.get(entry.getClassName());
      if (result == null) {
         result = defaultLevel;
      }
      return result.test(entry.getSeverity());
   }
}
