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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.lmpessoa.services.services.AbstractOptions;

class LoggerOptions extends AbstractOptions implements ILoggerOptions {

   static final LogWriter CONSOLE = new ConsoleLogWriter();

   private final Map<String, Function<LogEntry, String>> variables = new HashMap<>();
   private final Logger parent;

   private Map<String, Severity> packageLevels = new HashMap<>();
   private Severity defaultLevel = Severity.INFO;
   private LogWriter writer = CONSOLE;
   private LogFormatter formatter;

   LoggerOptions(Logger parent) {
      this.parent = parent;
      LogFormatParser.registerVariables(variables);
      try {
         useTemplate(FormattedLogWriter.DEFAULT);
      } catch (ParseException e) {
         // Ignore as it should never happen
         e.printStackTrace();
      }
   }

   @Override
   public void addVariable(String label, Function<LogEntry, String> func) {
      protectConfiguration();
      if (Objects.requireNonNull(label.isEmpty())) {
         throw new IllegalArgumentException("Variables must have a defined label");
      }
      if (variables.containsKey(label)) {
         throw new IllegalArgumentException("Cannot replace an existing variable");
      }
      variables.put(label, Objects.requireNonNull(func));
   }

   @Override
   public void setDefaultLevel(Severity level) {
      protectConfiguration();
      defaultLevel = Objects.requireNonNull(level);
      if (writer instanceof FormattedLogWriter) {
         ((FormattedLogWriter) writer).setTracing(defaultLevel == Severity.TRACE);
      }
   }

   @Override
   public void setPackageLevel(String packageName, Severity level) {
      protectConfiguration();
      Objects.requireNonNull(packageName);
      if (packageName.isEmpty() || packageName.matches("\\s")) {
         throw new IllegalArgumentException("Not a valid package name: " + packageName);
      }
      packageLevels.put(packageName, level);
   }

   @Override
   public void useTemplate(String template) throws ParseException {
      protectConfiguration();
      Objects.requireNonNull(template);
      formatter = LogFormatter.parse(template, variables);
      if (writer instanceof FormattedLogWriter) {
         ((FormattedLogWriter) writer).setFormatter(formatter);
      }
   }

   @Override
   public void useWriter(LogWriter writer) {
      protectConfiguration();
      this.writer = Objects.requireNonNull(writer);
      if (writer instanceof FormattedLogWriter) {
         ((FormattedLogWriter) writer).setTracing(defaultLevel == Severity.DEBUG);
         ((FormattedLogWriter) writer).setFormatter(formatter);
      }
   }

   @Override
   public void configurationEnded() {
      super.configurationEnded();
      parent.configurationEnded();
   }

   Map<String, Function<LogEntry, String>> getVariables() {
      return Collections.unmodifiableMap(variables);
   }

   LogWriter getWriter() {
      return writer;
   }

   boolean shouldLog(LogEntry entry) {
      String pckg = entry.getClassName();
      if (pckg != null) {
         while (pckg.lastIndexOf('.') > 0) {
            pckg = pckg.substring(0, pckg.lastIndexOf('.'));
            if (packageLevels.containsKey(pckg)) {
               Severity pckgLevel = packageLevels.get(pckg);
               return entry.getSeverity().compareTo(pckgLevel) <= 0;
            }
         }
      }
      return entry.getSeverity().compareTo(defaultLevel) <= 0;
   }
}
