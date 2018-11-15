/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.services.core.hosting;

import java.util.Enumeration;
import java.util.Objects;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

final class ApplicationConfig implements ServletConfig {

   private final ApplicationContext context;
   private final String servletName;

   @Override
   public String getServletName() {
      return servletName;
   }

   @Override
   public ServletContext getServletContext() {
      return context;
   }

   @Override
   public String getInitParameter(String name) {
      return context.getInitParameter(name);
   }

   @Override
   public Enumeration<String> getInitParameterNames() {
      return context.getInitParameterNames();
   }

   ApplicationConfig(ApplicationContext context, String servletName) {
      this.context = Objects.requireNonNull(context);
      this.servletName = servletName;
   }
}
