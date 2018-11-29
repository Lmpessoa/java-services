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

import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.StringLiteral;
import com.lmpessoa.services.views.liquid.parsing.Symbol;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.TagElementBuilder;

public final class CycleTagElement extends TagElement {

   private final String[] values;
   private final String name;

   protected CycleTagElement(String name, String[] values) {
      super("cycle");
      this.name = name != null ? name : Arrays.toString(values);
      this.values = values;
   }

   @Override
   public String render(LiquidRenderizationContext context) {
      RenderizationContextImpl ctx = (RenderizationContextImpl) context;
      int step = ctx.getSequence(name) % values.length;
      return values[step];
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("{% cycle ");
      if (name != null) {
         result.append('\'');
         result.append(name);
         result.append("': ");
      }
      result.append('\'');
      for (int i = 0; i < values.length; ++i) {
         if (i > 0) {
            result.append("', '");
         }
         result.append(values[i]);
      }
      result.append('\'');
      result.append(" %}");
      return result.toString();
   }

   public static final class Builder implements TagElementBuilder {

      @Override
      public TagElement build(String tagName, LiquidParseContext context) {
         StringLiteral value = context.readString(false);
         Collection<String> values = new ArrayList<>();
         String name = null;
         if (context.expectSymbol(Symbol.COLON, true)) {
            name = value.get();
            value = context.readString(false);
         }
         values.add(value.get());
         while (context.expectSymbol(Symbol.COMMA, true)) {
            value = context.readString(false);
            values.add(value.get());
         }
         return new CycleTagElement(name, values.toArray(new String[0]));
      }
   }
}
