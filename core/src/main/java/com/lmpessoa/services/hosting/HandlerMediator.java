/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
package com.lmpessoa.services.hosting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.lmpessoa.services.services.IServiceMap;
import com.lmpessoa.util.ClassUtils;

final class HandlerMediator {

   private static final String INVOKE = "invoke";

   private final List<Class<?>> handlers = new ArrayList<>();
   private final IServiceMap serviceMap;
   private final Method serviceInvoke;

   HandlerMediator(IServiceMap serviceMap) throws NoSuchMethodException {
      this.serviceInvoke = serviceMap.getClass().getDeclaredMethod(INVOKE, Object.class, String.class);
      this.serviceInvoke.setAccessible(true);
      this.serviceMap = serviceMap;
   }

   void addHandler(Class<?> handlerClass) {
      if (!ClassUtils.isConcreteClass(handlerClass) && Modifier.isPublic(handlerClass.getModifiers())) {
         throw new IllegalArgumentException("Handler must be a public concrete class");
      }
      if (ClassUtils.getConstructor(handlerClass, NextHandler.class) == null) {
         throw new IllegalArgumentException("Handler must implement a required constructor");
      }
      Method[] invokes = ClassUtils.findMethods(handlerClass, m -> INVOKE.equals(m.getName()));
      if (invokes.length != 1) {
         throw new IllegalArgumentException("Handler must have exaclty one method named 'invoke'");
      }
      if (handlers.isEmpty() && invokes[0].getReturnType() != HttpResult.class) {
         throw new IllegalArgumentException("First handler must return an HttpResult");
      }
      handlers.add(handlerClass);
   }

   Class<?> getHandler(int index) {
      return index >= 0 && index < handlers.size() ? handlers.get(index) : null;
   }

   Object invokeService(Object obj) throws IllegalAccessException, InvocationTargetException {
      try {
         return serviceInvoke.invoke(serviceMap, obj, INVOKE);
      } catch (InvocationTargetException e) {
         while (e.getCause() instanceof InvocationTargetException) {
            e = (InvocationTargetException) e.getCause();
         }
         throw e;
      }
   }

   HttpResult invoke() {
      NextHandler next = new NextHandler(this, 0);
      return (HttpResult) next.invoke();
   }
}
