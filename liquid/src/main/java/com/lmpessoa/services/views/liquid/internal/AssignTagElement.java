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
import com.lmpessoa.services.views.liquid.parsing.Expression;
import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.Symbol;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.TagElementBuilder;
import com.lmpessoa.services.views.liquid.parsing.Variable;

public final class AssignTagElement extends TagElement {

   private final Expression expr;
   private final String varName;

   protected AssignTagElement(String varName, Expression expr) {
      super("assign");
      this.varName = varName;
      this.expr = expr;
   }

   @Override
   public String render(LiquidRenderizationContext context) {
      context.data().set(varName, expr.getValue(context));
      return "";
   }

   @Override
   public String toString() {
      return String.format("{%% assign %s = %s %%}", varName, expr);
   }

   public static final class Builder implements TagElementBuilder {

      @Override
      public TagElement build(String tagName, LiquidParseContext context) {
         Variable var = context.readSimpleVariable(false);
         context.expectSymbol(Symbol.ASSIGN, false);
         Expression expr = context.readFilteredExpression(false);
         return new AssignTagElement(var.name(), expr);
      }
   }
}
