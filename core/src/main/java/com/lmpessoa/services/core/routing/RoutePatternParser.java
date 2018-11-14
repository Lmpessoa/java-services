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
package com.lmpessoa.services.core.routing;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lmpessoa.services.util.parsing.AbstractParser;
import com.lmpessoa.services.util.parsing.ITemplatePart;

final class RoutePatternParser extends AbstractParser<AbstractRouteType> {

   private static final Pattern pattern = Pattern.compile("^([a-z]+)(\\(([0-9]+)?(..)?([0-9]+)?\\))?$");
   private final RouteOptions options;

   RoutePatternParser(String template, RouteOptions options) {
      super(template, true, null);
      this.options = options;
   }

   public static List<ITemplatePart> parse(String template, RouteOptions options) throws ParseException {
      return new RoutePatternParser(template, options).parse();
   }

   @Override
   protected AbstractRouteType parseVariable(int pos, String variablePart) throws ParseException {
      Matcher matcher = pattern.matcher(variablePart);
      if (!matcher.find()) {
         throw new ParseException("Invalid variable definition", pos);
      }
      String varType = matcher.group(1);
      Class<? extends AbstractRouteType> type = options.getTypes().get(varType);
      if (type == null) {
         throw new ParseException("Unknown route type: " + varType, pos);
      }
      Object[] args;
      if (matcher.group(2) != null) {
         String minlenStr = matcher.group(3);
         String maxlenStr = matcher.group(5);
         if (minlenStr == null && maxlenStr == null) {
            throw new ParseException("Length range expected", pos);
         }
         int minlen = minlenStr == null ? 1 : Integer.parseInt(minlenStr);
         int maxlen = maxlenStr == null ? -1 : Integer.parseInt(maxlenStr);
         if (maxlen == -1 && matcher.group(4) == null) {
            args = new Object[] { minlen };
         } else {
            args = new Object[] { minlen, maxlen };
         }
      } else {
         args = new Object[0];
      }
      Class<?>[] argTypes = new Class<?>[args.length];
      Arrays.fill(argTypes, int.class);
      try {
         Constructor<? extends AbstractRouteType> constructor = type.getConstructor(argTypes);
         AbstractRouteType result = constructor.newInstance(args);
         result.setRouteOptions(options);
         return result;
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException
               | InvocationTargetException e) {
         throw new ParseException(e.getMessage(), pos);
      }
   }
}
