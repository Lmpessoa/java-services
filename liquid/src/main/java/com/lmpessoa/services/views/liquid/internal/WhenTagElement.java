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
import java.util.Collection;

import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.parsing.BreakTagElement;
import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.Literal;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.TagElementBuilder;

public final class WhenTagElement extends BreakTagElement {

   final Literal<?>[] values;

   protected WhenTagElement(Literal<?>[] values) {
      super("when");
      this.values = values;
   }

   @Override
   public String render(LiquidRenderizationContext context) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("{% when ");
      for (int i = 0; i < values.length; ++i) {
         if (i > 0) {
            result.append(" or ");
         }
         result.append(values[i]);
      }
      result.append(" %}");
      return result.toString();
   }

   public static final class Builder implements TagElementBuilder {

      @Override
      public TagElement build(String tagName, LiquidParseContext context) {
         Collection<Literal<?>> values = new ArrayList<>();
         values.add(context.readLiteral(false));
         while (context.expectIdentifier("or", true)) {
            values.add(context.readLiteral(false));
         }
         return new WhenTagElement(values.toArray(new Literal<?>[0]));
      }
   }
}
