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
package com.lmpessoa.services.views.liquid.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.lmpessoa.services.views.liquid.LiquidFilterFunction;
import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.parsing.Expression;

final class FilteredExpression implements Expression {

   private final Collection<Filter> filters = new ArrayList<>();
   private final Expression expr;

   @Override
   public Object getValue(LiquidRenderizationContext context) {
      Object value = expr.getValue(context);
      for (Filter filter : filters) {
         LiquidFilterFunction func = ((RenderizationContextImpl) context).getFilter(filter.name);
         Object[] args = Arrays.stream(filter.args).map(e -> e.getValue(context)).toArray();
         value = func.apply(value, args);
      }
      return value;
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      result.append(expr);
      for (Filter filter : filters) {
         result.append(" | ");
         result.append(filter.name);
         if (filter.args.length > 0) {
            result.append(": ");
            for (int i = 0; i < filter.args.length; ++i) {
               if (i > 0) {
                  result.append(", ");
               }
               result.append(filter.args[i]);
            }
         }
      }
      return result.toString();
   }

   FilteredExpression(Expression expr) {
      this.expr = expr;
   }

   void addFilter(String filterName, Expression[] args) {
      filters.add(new Filter(filterName, args));
   }

   private static class Filter {

      private final Expression[] args;
      private final String name;

      public Filter(String name, Expression[] args) {
         this.name = name;
         this.args = args;
      }
   }
}
