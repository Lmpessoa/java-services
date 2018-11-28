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
package com.lmpessoa.services.internal.routing;

import java.lang.reflect.Executable;
import java.util.List;

import com.lmpessoa.services.internal.parsing.AbstractParser;
import com.lmpessoa.services.internal.parsing.ITemplatePart;
import com.lmpessoa.services.internal.parsing.ParseException;

final class RoutePatternParser extends AbstractParser<VariableRoutePart> {

   private final Class<?> resourceClass;
   private final Executable exec;

   RoutePatternParser(Class<?> resourceClass, Executable exec, String template) {
      super(template, true, null);
      this.resourceClass = resourceClass;
      this.exec = exec;
   }

   public static List<ITemplatePart> parse(Class<?> resourceClass, Executable exec,
      String template) {
      return new RoutePatternParser(resourceClass, exec, template).parse();
   }

   @Override
   protected VariableRoutePart parseVariable(int pos, String variablePart) {
      if (!variablePart.matches("\\d+")) {
         for (int i = 0; i < exec.getParameterCount(); ++i) {
            if (exec.getParameters()[i].getName().equals(variablePart)) {
               variablePart = String.valueOf(i);
               break;
            }
         }
      }
      try {
         return new VariableRoutePart(resourceClass, exec, Integer.parseInt(variablePart));
      } catch (Exception e) {
         throw new ParseException(e.getMessage(), pos);
      }
   }
}
