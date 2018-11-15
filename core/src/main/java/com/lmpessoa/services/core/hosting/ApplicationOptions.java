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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;

final class ApplicationOptions implements IApplicationOptions {

   private static final List<Class<?>> LAST_HANDLERS = Arrays.asList(InvokeHandler.class);
   private final List<Class<?>> handlers = new ArrayList<>();

   ApplicationOptions() {
      handlers.add(ResultHandler.class);
      handlers.add(FaviconHandler.class);
   }

   @Override
   public void useHandler(Class<?> handlerClass) {
      if (handlers.contains(handlerClass) || LAST_HANDLERS.contains(handlerClass)) {
         throw new IllegalArgumentException("Handler is already registered");
      }
      if (!ClassUtils.isConcreteClass(handlerClass)) {
         throw new IllegalArgumentException("Handler must be a concrete class");
      }
      if (ClassUtils.getConstructor(handlerClass, NextHandler.class) == null) {
         throw new IllegalArgumentException("Handler must implement a required constructor");
      }
      Method[] invokes = ClassUtils.findMethods(handlerClass, m -> "invoke".equals(m.getName()));
      if (invokes.length != 1) {
         throw new IllegalArgumentException("Handler must have exaclty one method named 'invoke'");
      }
      handlers.add(handlerClass);
   }

   NextHandler getFirstResponder(ServiceMap services) {
      List<Class<?>> allHandlers = new ArrayList<>();
      allHandlers.addAll(this.handlers);
      allHandlers.addAll(LAST_HANDLERS);
      return new NextHandler(services, allHandlers);
   }
}
