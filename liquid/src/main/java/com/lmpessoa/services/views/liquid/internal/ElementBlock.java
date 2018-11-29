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
import com.lmpessoa.services.views.liquid.parsing.Element;
import com.lmpessoa.services.views.liquid.parsing.TagElement;

final class ElementBlock implements Element {

   private final Element[] elements;

   ElementBlock(Element[] elements) {
      this.elements = elements;
   }

   @Override
   public String toString() {
      return Arrays.toString(elements);
   }

   @Override
   public String render(LiquidRenderizationContext context) {
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < elements.length; ++i) {
         String render = null;
         if (elements[i] instanceof BlockTagElement) {
            String endName = "end" + ((TagElement) elements[i]).name();
            for (int j = i + 1; j < elements.length; ++j) {
               if (elements[j] instanceof TagElement
                        && endName.equals(((TagElement) elements[j]).name())) {
                  Element[] block = Arrays.copyOfRange(elements, i, j + 1);
                  render = ((BlockTagElement) elements[i]).render(context, block);
                  i = j;
                  break;
               }
            }
         } else {
            render = elements[i].render(context);
         }
         if (render != null) {
            result.append(render);
         }
         if (((RenderizationContextImpl) context).isInterrupted()) {
            break;
         }
      }
      return result.toString();
   }
}
