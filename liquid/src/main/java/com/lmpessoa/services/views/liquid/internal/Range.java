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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.parsing.Value;

final class Range implements Value {

   private final Value right;
   private final Value left;

   Range(Value left, Value right) {
      this.right = right;
      this.left = left;
   }

   @Override
   public Object getValue(LiquidRenderizationContext context) {
      BigDecimal vleft = (BigDecimal) left.getValue(context);
      BigDecimal vright = (BigDecimal) right.getValue(context);
      Collection<BigDecimal> result = new ArrayList<>();
      while (vleft.compareTo(vright) <= 0) {
         result.add(vleft);
         vleft = vleft.add(BigDecimal.ONE);
      }
      return result.toArray();
   }

   @Override
   public String toString() {
      return String.format("(%s..%s)", left, right);
   }
}
