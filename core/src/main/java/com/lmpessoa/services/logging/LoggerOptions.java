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

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.lmpessoa.services.services.AbstractOptions;

class LoggerOptions extends AbstractOptions implements ILoggerOptions {

   private ZoneId zone = ZonedDateTime.now().getZone();
   private LogWriter writer = new ConsoleLogWriter();
   private Map<String, Severity> packageLevels = new HashMap<>();
   private Severity defaultLevel = Severity.INFO;
   private LogFormatter formatter;

   LoggerOptions() {
      try {
         useTemplate(ILoggerOptions.DEFAULT);
      } catch (ParseException e) {
         // Ignore as it should never happen
         e.printStackTrace();
      }
   }

   @Override
   public void setDefaultLevel(Severity level) {
      defaultLevel = Objects.requireNonNull(level);
   }

   @Override
   public void setPackageLevel(String packageName, Severity level) {
      Objects.requireNonNull(packageName);
      if (packageName.isEmpty() || packageName.matches("\\s")) {
         throw new IllegalArgumentException("Not a valid package name: " + packageName);
      }
      packageLevels.put(packageName, level);
   }

   @Override
   public void useTemplate(String template) throws ParseException {
      Objects.requireNonNull(template);
      formatter = LogFormatter.parse(template);
      if (writer instanceof FormattedLogWriter) {
         ((FormattedLogWriter) writer).setFormatter(formatter);
      }
   }

   @Override
   public void useWriter(LogWriter writer) {
      this.writer = Objects.requireNonNull(writer);
      if (writer instanceof FormattedLogWriter) {
         ((FormattedLogWriter) writer).setFormatter(formatter);
      }
   }

   @Override
   public void useTimeZone(ZoneId zone) {
      this.zone = Objects.requireNonNull(zone);
   }

   void process(LogEntry entry) {
      if (shouldLog(entry) && writer != null) {
         writer.append(entry);
      }
   }

   ZoneId getLogZone() {
      return zone;
   }

   private boolean shouldLog(LogEntry entry) {
      String pckg = entry.getClassName();
      while (pckg.lastIndexOf('.') > 0) {
         pckg = pckg.substring(0, pckg.lastIndexOf('.'));
         if (packageLevels.containsKey(pckg)) {
            Severity pckgLevel = packageLevels.get(pckg);
            return entry.getSeverity().compareTo(pckgLevel) >= 0;
         }
      }
      return defaultLevel.compareTo(entry.getSeverity()) >= 0;
   }
}
