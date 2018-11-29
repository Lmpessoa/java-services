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
import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.Literal;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.Variable;

public final class CaseTagElement extends BlockTagElement {

   private final Variable var;

   protected CaseTagElement(Variable var) {
      super("case");
      this.var = var;
   }

   @Override
   public String render(LiquidRenderizationContext context, Element[] elements) {
      final Object value = var.getValue(context);
      return context.doBlock(() -> {
         String pre = elements[1].render(context);
         for (int i = 2; i < elements.length; i += 2) {
            if (elements[i] instanceof WhenTagElement) {
               Literal<?>[] values = ((WhenTagElement) elements[i]).values;
               if (Arrays.stream(values).map(Literal::get).anyMatch(v -> v.equals(value))) {
                  return pre + elements[i + 1].render(context);
               }
            } else if ("else".equals(((TagElement) elements[i]).name())) {
               return pre + elements[i + 1].render(context);
            }
         }
         return pre;
      });
   }

   @Override
   public String toString() {
      return String.format("{%% case %s %%}", var.name());
   }

   public static final class Builder implements BlockTagElementBuilder {

      private boolean foundElse = false;

      @Override
      public TagElement build(String tagName, LiquidParseContext context) {
         return new CaseTagElement(context.readVariable(false));
      }

      @Override
      public boolean acceptsBreak(BreakTagElement tag) {
         boolean result = !foundElse && Arrays.asList("when", "else").contains(tag.name());
         foundElse = "else".equals(tag.name());
         return result;
      }
   }
}
