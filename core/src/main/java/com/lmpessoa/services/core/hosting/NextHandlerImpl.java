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
package com.lmpessoa.services.core.hosting;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.lmpessoa.services.core.services.NoSingleMethodException;
import com.lmpessoa.services.core.services.ServiceMap;

final class NextHandlerImpl implements NextHandler {

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
      NextHandler next = new NextHandlerImpl(services, handlers.subList(1, handlers.size()));
      try {
         Constructor<?> constructor = handlerClass.getConstructor(NextHandler.class);
         Object handler = constructor.newInstance(next);
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

   NextHandlerImpl(ServiceMap services, List<Class<?>> handlers) {
      this.services = services;
      this.handlers = handlers;
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