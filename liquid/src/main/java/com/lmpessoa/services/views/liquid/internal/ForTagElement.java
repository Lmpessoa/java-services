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
import com.lmpessoa.services.views.liquid.parsing.Expression;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.Value;
import com.lmpessoa.services.views.liquid.parsing.Variable;

public final class ForTagElement extends LoopTagElement {

   @Override
   public String render(LiquidRenderizationContext context, Element[] elements) {
      RenderizationContextImpl ctx = (RenderizationContextImpl) context;
      Object[] values = LiquidParser.arrayOf(expr.getValue(context));
      if (values.length > 0) {
         StringBuilder result = new StringBuilder();
         final int limit = this.limit == null ? 0 : intOf(this.limit, context);
         final int offset = this.offset == null ? 0 : intOf(this.offset, context);
         final int count = Math.min(values.length, limit == 0 ? values.length : offset + limit);
         for (int i = offset; i < count; ++i) {
            ctx.resetLoop();
            Object obj = values[i];
            ForLoop forloop = new ForLoop(values.length, i, i == offset, i == count - 1);
            String loopResult = context.doBlock(() -> {
               context.data().set(var.name(), obj);
               context.data().set("forloop", forloop);
               return elements[1].render(context);
            });
            result.append(loopResult);
            if (!ctx.shouldContinue()) {
               break;
            }
         }
         return result.toString();
      } else if ("else".equals(((TagElement) elements[2]).name())) {
         return elements[3].render(context);
      }
      return null;
   }

   protected ForTagElement(Variable var, Expression expr, Value offset, Value limit,
      boolean reversed) {
      super("for", var, expr, offset, limit, reversed);
   }

   private int intOf(Value value, LiquidRenderizationContext context) {
      Object obj = value.getValue(context);
      if (obj instanceof Number) {
         return ((Number) obj).intValue();
      } else if (obj instanceof String) {
         try {
            return Integer.parseInt((String) obj);
         } catch (Exception e) {
            // Do nothing here
         }
      }
      throw new IllegalArgumentException(
               "Expected a number, was " + obj.getClass().getName() + ": " + value.toString());
   }

   public static final class Builder extends LoopTagElement.Builder {

      private boolean foundElse = false;

      @Override
      public boolean acceptsBreak(BreakTagElement tag) {
         boolean result = !foundElse && "else".equals(tag.name());
         foundElse = "else".equals(tag.name());
         return result;

      }

      @Override
      protected TagElement create(Variable var, Value list, Value offset, Value limit,
         boolean reversed) {
         return new ForTagElement(var, list, offset, limit, reversed);
      }
   }

   @SuppressWarnings("unused")
   private static final class ForLoop {

      private final boolean first;
      private final boolean last;
      private final int length;
      private final int index;

      ForLoop(int length, int index, boolean first, boolean last) {
         this.length = length;
         this.index = index;
         this.first = first;
         this.last = last;
      }

      public int getLength() {
         return length;
      }

      public int getIndex() {
         return index + 1;
      }

      public int getIndex0() {
         return index;
      }

      public int getRindex() {
         return length - index - 1;
      }

      public int getRindex0() {
         return length - index - 1;
      }

      public boolean isFirst() {
         return first;
      }

      public boolean isLast() {
         return last;
      }
   }
}
