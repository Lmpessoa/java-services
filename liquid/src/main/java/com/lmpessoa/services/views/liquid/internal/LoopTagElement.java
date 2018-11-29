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

import com.lmpessoa.services.views.liquid.parsing.BlockTagElement;
import com.lmpessoa.services.views.liquid.parsing.BlockTagElementBuilder;
import com.lmpessoa.services.views.liquid.parsing.Expression;
import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.Symbol;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.Value;
import com.lmpessoa.services.views.liquid.parsing.Variable;

abstract class LoopTagElement extends BlockTagElement {

   protected final Expression expr;
   protected final Variable var;

   protected final boolean reversed;
   protected final Value offset;
   protected final Value limit;

   protected LoopTagElement(String name, Variable var, Expression expr, Value offset, Value limit,
      boolean reversed) {
      super(name);
      this.reversed = reversed;
      this.offset = offset;
      this.limit = limit;
      this.expr = expr;
      this.var = var;
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("{% ");
      result.append(name());
      result.append(' ');
      result.append(var.name());
      result.append(" in ");
      result.append(expr);
      if (limit != null) {
         result.append(" limit: ");
         result.append(limit);
      }
      if (offset != null) {
         result.append(" offset: ");
         result.append(offset);
      }
      if (reversed) {
         result.append(" reversed");
      }
      result.append(" %}");
      return result.toString();
   }

   static abstract class Builder implements BlockTagElementBuilder {

      @Override
      public final TagElement build(String tagName, LiquidParseContext context) {
         Variable var = context.readSimpleVariable(false);
         context.expectIdentifier("in", false);
         Value list = context.readRangeOrVariable(true);
         Value offset = null;
         Value limit = null;
         boolean reversed = false;
         while (true) {
            if (context.expectIdentifier("limit", true)) {
               context.expectSymbol(Symbol.COLON, false);
               limit = context.readNumberOrVariable(false);
            } else if (context.expectIdentifier("offset", true)) {
               context.expectSymbol(Symbol.COLON, false);
               offset = context.readNumberOrVariable(false);
            } else if (context.expectIdentifier("reversed", true)) {
               reversed = true;
            } else {
               break;
            }
         }
         return create(var, list, offset, limit, reversed);
      }

      protected abstract TagElement create(Variable var, Value list, Value offset, Value limit,
         boolean reversed);
   }
}
