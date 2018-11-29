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

import java.util.Arrays;

import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.parsing.BlockTagElement;
import com.lmpessoa.services.views.liquid.parsing.BlockTagElementBuilder;
import com.lmpessoa.services.views.liquid.parsing.BreakTagElement;
import com.lmpessoa.services.views.liquid.parsing.Element;
import com.lmpessoa.services.views.liquid.parsing.Expression;
import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.TagElement;

public final class IfTagElement extends BlockTagElement {

   private final Expression expr;

   protected IfTagElement(String name, Expression expr) {
      super(name);
      this.expr = expr;
   }

   @Override
   public String render(LiquidRenderizationContext context, Element[] elements) {
      for (int i = 0; i < elements.length; i += 2) {
         Expression expr = null;
         Object value = null;
         String tagName = null;
         if (elements[i] instanceof IfTagElement) {
            expr = ((IfTagElement) elements[i]).expr;
            value = expr.getValue(context);
         } else if (elements[i] instanceof TagElement) {
            tagName = ((TagElement) elements[i]).name();
         }
         if (expr != null && value != null && value != Boolean.FALSE || "else".equals(tagName)) {
            final Element element = elements[i + 1];
            return context.doBlock(() -> element.render(context));
         }
      }
      return "";
   }

   @Override
   public String toString() {
      return String.format("{%% %s %s %%}", name(), expr);
   }

   public static class Builder implements BlockTagElementBuilder {

      private boolean foundElse = false;

      @Override
      public TagElement build(String tagName, LiquidParseContext context) {
         return new IfTagElement(tagName, context.readExpression(false));
      }

      @Override
      public boolean acceptsBreak(BreakTagElement tag) {
         boolean result = !foundElse && Arrays.asList("elsif", "else").contains(tag.name());
         foundElse = "else".equals(tag.name());
         return result;
      }
   }
}
