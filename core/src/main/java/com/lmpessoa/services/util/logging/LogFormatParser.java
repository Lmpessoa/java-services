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

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lmpessoa.services.util.parsing.AbstractParser;
import com.lmpessoa.services.util.parsing.ITemplatePart;

final class LogFormatParser extends AbstractParser<LogVariable> {

   private static final Pattern VARIABLE_DEF = Pattern
            .compile("^([A-Z][a-zA-Z0-9]*(?:\\.[A-Z0-9][a-zA-Z0-9]*)*)(?:\\:([<>])(\\d+))?$");
   private final Map<String, Function<LogEntry, String>> variables;

   @Override
   protected LogVariable parseVariable(int pos, String variablePart) throws ParseException {
      Matcher matcher = VARIABLE_DEF.matcher(variablePart);
      if (!matcher.find()) {
         throw new ParseException("Not a valid variable reference: " + variablePart, pos);
      }
      final String varName = matcher.group(1);
      Function<LogEntry, String> func = variables.get(varName);
      if (func == null && !"Class.Name".equals(varName)) {
         throw new ParseException("Unknown log variable: " + varName, pos);
      }
      boolean rightAlign = ">".equals(matcher.group(2));
      int length = matcher.group(3) != null ? Integer.valueOf(matcher.group(3)) : -1;
      if ("Class.Name".equals(varName)) {
         return new ClassNameLogVariable(varName, rightAlign, length, func);
      }
      return new LogVariable(varName, rightAlign, length, func);
   }

   static List<ITemplatePart> parse(String template, Map<String, Function<LogEntry, String>> variables)
      throws ParseException {
      return new LogFormatParser(template, variables).parse();
   }

   private LogFormatParser(String template, Map<String, Function<LogEntry, String>> variables) {
      super(template, false, null);
      this.variables = variables;
   }
}
