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

import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.parsing.BreakTagElement;
import com.lmpessoa.services.views.liquid.parsing.Element;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.Value;
import com.lmpessoa.services.views.liquid.parsing.Variable;

public final class TablerowTagElement extends LoopTagElement {

   protected TablerowTagElement(Variable var, Value expr, Value offset, Value limit,
      boolean reversed) {
      super("tablerow", var, expr, offset, limit, reversed);
   }

   @Override
   public String render(LiquidRenderizationContext context, Element[] elements) {
      // TODO Auto-generated method stub
      return null;
   }

   public static final class Builder extends LoopTagElement.Builder {

      @Override
      public boolean acceptsBreak(BreakTagElement tag) {
         return false;
      }

      @Override
      protected TagElement create(Variable var, Value list, Value offset, Value limit,
         boolean reversed) {
         return new TablerowTagElement(var, list, offset, limit, reversed);
      }
   }
}
