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
package com.lmpessoa.services.core.internal.hosting;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.internal.services.NoSingleMethodException;
import com.lmpessoa.services.core.internal.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;

final class NextResponderImpl implements NextResponder {

   private final ApplicationOptions options;
   private final List<Class<?>> handlers;
   private final ServiceMap services;

   private boolean invoked = false;

   @Override
   public Object invoke() {
      if (invoked) {
         throw new IllegalStateException("Next handler has already been called");
      }
      invoked = true;
      Class<?> handlerClass = handlers.get(0);
      if (handlerClass == null) {
         return null;
      }
      NextResponder next = new NextResponderImpl(services, handlers.subList(1, handlers.size()),
               options);
      try {
         Constructor<?> constructor = findConstructor(handlerClass);
         Object[] params = new Object[] { next };
         if (constructor.getParameterCount() == 2) {
            params = Arrays.copyOf(params, 2);
            params[1] = options;
         }
         Object handler = constructor.newInstance(params);
         return invokeService(handler);
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof HttpException) {
            throw (HttpException) e.getCause();
         }
         if (e.getCause() instanceof InternalServerError) {
            throw (InternalServerError) e.getCause();
         }
         throw new InternalServerError(e.getCause());
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }

   NextResponderImpl(ServiceMap services, List<Class<?>> handlers, ApplicationOptions options) {
      this.services = services;
      this.handlers = handlers;
      this.options = options;
   }

   private Constructor<?> findConstructor(Class<?> handlerClass) {
      Constructor<?> result = ClassUtils.getConstructor(handlerClass, NextResponder.class,
               ApplicationOptions.class);
      if (result == null) {
         result = ClassUtils.getConstructor(handlerClass, NextResponder.class);
      }
      return result;
   }

   private Object invokeService(Object obj) throws IllegalAccessException,
      InvocationTargetException, NoSingleMethodException, InstantiationException {
      try {
         return services.invoke(obj, "invoke");
      } catch (InvocationTargetException e) {
         InvocationTargetException ex = e;
         while (ex.getCause() instanceof InvocationTargetException) {
            ex = (InvocationTargetException) ex.getCause();
         }
         throw ex;
      }
   }
}
