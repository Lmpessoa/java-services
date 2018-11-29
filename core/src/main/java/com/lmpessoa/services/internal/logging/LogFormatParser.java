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
package com.lmpessoa.services.internal.logging;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.internal.parsing.AbstractParser;
import com.lmpessoa.services.internal.parsing.ITemplatePart;
import com.lmpessoa.services.internal.parsing.ParseException;

final class LogFormatParser extends AbstractParser<LogVariable> {

   private static final Pattern VARIABLE_DEF = Pattern
            .compile("^([A-Z][a-zA-Z0-9]*(?:\\.[A-Z0-9][a-zA-Z0-9]*)*)(?:\\:([<>])(\\d+))?$");

   @Override
   protected LogVariable parseVariable(int pos, String variablePart) {
      Matcher matcher = VARIABLE_DEF.matcher(variablePart);
      if (!matcher.find()) {
         throw new ParseException(CoreMessage.INVALID_VARIABLE_REFERENCE.with(variablePart), pos);
      }
      final String varName = matcher.group(1);
      boolean rightAlign = ">".equals(matcher.group(2));
      int length = matcher.group(3) != null ? Integer.valueOf(matcher.group(3)) : -1;
      if ("Class.Name".equals(varName)) {
         return new ClassNameLogVariable(varName, rightAlign, length);
      }
      return new LogVariable(varName, rightAlign, length);
   }

   static List<ITemplatePart> parse(String template) {
      return new LogFormatParser(template).parse();
   }

   private LogFormatParser(String template) {
      super(template, false, null);
   }
}
