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
package com.lmpessoa.services.views;

import java.io.Serializable;
import java.util.Objects;

import com.lmpessoa.services.views.templating.RenderizationContext;

public final class ViewAndModel implements Serializable {

   private static final long serialVersionUID = 1L;

   private final transient RenderizationContext context = new RenderizationContext();
   private final transient String viewName;

   public ViewAndModel(String viewName) {
      Objects.requireNonNull(viewName);
      if (viewName.startsWith("../") || viewName.contains("/../")) {
         throw new IllegalArgumentException(ViewsMessage.RELATIVE_VIEW_PATH.get());
      }
      while (viewName.startsWith("/")) {
         viewName = viewName.substring(1);
      }
      while (viewName.endsWith("/")) {
         viewName = viewName.substring(0, viewName.length() - 1);
      }
      this.viewName = viewName.replace("//", "/");
   }

   public ViewAndModel(String viewName, Object modelObject) {
      this(viewName);
      set("model", modelObject);
   }

   public void set(String name, Object value) {
      context.set(name, value);
   }

   String getViewName() {
      return viewName;
   }

   RenderizationContext getContext() {
      return context;
   }
}
