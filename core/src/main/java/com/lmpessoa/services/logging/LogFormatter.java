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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.lmpessoa.util.parsing.ITemplatePart;
import com.lmpessoa.util.parsing.LiteralPart;

final class LogFormatter {

   private final List<ITemplatePart> parts;

   static LogFormatter parse(String template, Map<String, Function<LogEntry, String>> variable) throws ParseException {
      return new LogFormatter(LogFormatParser.parse(template, variable));
   }

   String format(LogEntry entry) {
      return format(entry, null);
   }

   String format(LogEntry entry, String message) {
      StringBuilder result = new StringBuilder();
      for (ITemplatePart part : parts) {
         if (part instanceof LogVariable) {
            LogVariable var = (LogVariable) part;
            if (var.isMessage() && message != null) {
               result.append(message);
            } else {
               result.append(var.getValueOf(entry));
            }
         } else {
            result.append(((LiteralPart) part).getValue());
         }
      }
      return result.toString();
   }

   private LogFormatter(List<ITemplatePart> parts) {
      this.parts = parts;
   }
}
