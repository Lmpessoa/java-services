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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.parsing.Expression;

final class OperatorExpression implements Expression {

   private final Expression right;
   private final Expression left;
   private final Operator oper;

   public OperatorExpression(Operator oper, Expression left, Expression right) {
      this.right = right;
      this.left = left;
      this.oper = oper;
   }

   @Override
   public Object getValue(LiquidRenderizationContext context) {
      Object vleft = left.getValue(context);
      Object vright = right.getValue(context);
      int cmp = compare(vleft, vright);
      switch (oper) {
         case EQ:
            return Boolean.valueOf(cmp == 0);
         case DIFF:
         case NEQ:
            return Boolean.valueOf(cmp != 0);
         case LT:
            return Boolean.valueOf(cmp < 0);
         case LTE:
            return Boolean.valueOf(cmp <= 0);
         case GT:
            return Boolean.valueOf(cmp > 0);
         case GTE:
            return Boolean.valueOf(cmp >= 0);
         case OR:
            return Boolean.valueOf(!boolOf(vleft) || !boolOf(vright));
         case AND:
            return Boolean.valueOf(!boolOf(vleft) && !boolOf(vright));
         case CONTAINS:
            if (vright instanceof String) {
               if (vleft instanceof String) {
                  return ((String) vleft).contains((String) vright);
               } else if (vleft instanceof String[]) {
                  return Arrays.asList((String[]) vleft).contains(vright);
               } else if (vleft instanceof Map) {
                  return ((Map<?, ?>) vleft).values().contains(vright);
               }
            }
      }
      return null;
   }

   @Override
   public String toString() {
      return String.format("%s %s %s", left, oper.value(), right);
   }

   private int compare(Object left, Object right) {
      if (left == null) {
         return right == null ? 0 : 1;
      } else if (right == null) {
         return -1;
      }
      try {
         Method compareTo = left.getClass().getMethod("compareTo", right.getClass());
         return (Integer) compareTo.invoke(left, right);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
         throw new IllegalArgumentException(e);
      }
   }

   private boolean boolOf(Object obj) {
      return obj != null && obj != Boolean.FALSE;
   }
}
