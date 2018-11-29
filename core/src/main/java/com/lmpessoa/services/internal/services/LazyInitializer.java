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
package com.lmpessoa.services.internal.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.services.Reuse;

final class LazyInitializer<T> implements Supplier<T> {

   private final Class<? extends T> provider;
   private final ServiceMap serviceMap;

   public LazyInitializer(Class<? extends T> provider, Reuse level, ServiceMap serviceMap) {
      this.serviceMap = serviceMap;
      this.provider = provider;
      if (!Modifier.isPublic(provider.getModifiers()) || !ClassUtils.isConcreteClass(provider)) {
         throw new IllegalArgumentException(CoreMessage.SERVICE_NOT_CONCRETE.get());
      }
      final Constructor<?>[] constructors = provider.getDeclaredConstructors();
      if (constructors.length != 1) {
         throw new IllegalArgumentException(new NoSingleMethodException(
                  CoreMessage.TOO_MANY_CONSTRUCTORS.with(provider.getName(), constructors.length)));
      }
      for (Class<?> paramType : constructors[0].getParameterTypes()) {
         ServiceEntry entry = this.serviceMap.getEntry(paramType);
         if (entry == null) {
            throw new IllegalArgumentException(
                     CoreMessage.SERVICE_DEPENDENCY.with(provider.getName(), paramType.getName()));

         }
         if (entry.getLevel().compareTo(level) < 0) {
            throw new IllegalArgumentException(CoreMessage.SERVICE_LOWER_LIFETIME
                     .with(provider.getName(), paramType.getName()));
         }
      }
   }

   @Override
   @SuppressWarnings("unchecked")
   public T get() {
      try {
         Constructor<?> constructor = provider.getDeclaredConstructors()[0];
         return (T) serviceMap.invoke(null, constructor);
      } catch (InvocationTargetException e) {
         throw new LazyInstatiationException(e.getCause());
      } catch (Exception e) {
         throw new LazyInstatiationException(e);
      }
   }
}
