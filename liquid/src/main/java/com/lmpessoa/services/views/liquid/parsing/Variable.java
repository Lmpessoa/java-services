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
package com.lmpessoa.services.views.liquid.parsing;

import java.math.BigDecimal;
import java.util.function.Function;

import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.internal.LiquidParser;

public final class Variable implements Value {

   private final String name;

   public Variable(String name) {
      this.name = name;
   }

   public String name() {
      return name;
   }

   @Override
   public String toString() {
      return name;
   }

   @Override
   public Object getValue(LiquidRenderizationContext context) {
      String name = this.name;
      Function<Object, Object> filter = null;
      if (name.endsWith(".first")) {
         name = name.substring(0, name.length() - 6);
         filter = Variable::first;
      } else if (name.endsWith(".last")) {
         name = name.substring(0, name.length() - 5);
         filter = Variable::last;
      } else if (name.endsWith(".size")) {
         name = name.substring(0, name.length() - 5);
         filter = Variable::size;
      }
      Object result = context.data().get(name);
      if (result != null && filter != null) {
         result = filter.apply(result);
      }
      if (result instanceof Number && !(result instanceof BigDecimal)) {
         result = new BigDecimal(result.toString());
      }
      return result;
   }

   private static Object first(Object value) {
      Object[] values = LiquidParser.arrayOf(value);
      if (values != null && values.length > 0) {
         value = values[0];
         return value.getClass().isArray() ? ((Object[]) value)[1] : value;
      }
      return value;
   }

   private static Object last(Object value) {
      Object[] values = LiquidParser.arrayOf(value);
      if (values != null && values.length > 0) {
         value = values[values.length - 1];
         return value.getClass().isArray() ? ((Object[]) value)[1] : value;
      }
      return value;
   }

   private static Object size(Object value) {
      if (value instanceof String) {
         return ((String) value).length();
      }
      Object[] values = LiquidParser.arrayOf(value);
      if (value != null) {
         return values.length;
      }
      return null;
   }
}
