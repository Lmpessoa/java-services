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
package com.lmpessoa.services.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Supplier;

import com.lmpessoa.util.ClassUtils;

final class LazyInitializer<T> implements Supplier<T> {

   private final Class<? extends T> provider;
   private final IServiceMap serviceMap;

   public LazyInitializer(Class<? extends T> provider, ReuseLevel level, IServiceMap serviceMap) {
      if (!ClassUtils.isConcreteClass(provider)) {
         throw new IllegalArgumentException("Provider class must be a concrete class");
      }
      final Constructor<?>[] constructors = provider.getConstructors();
      if (constructors.length != 1) {
         throw new IllegalArgumentException(new NoSingleMethodException(
                  "Provider class must have only one constructor", constructors.length));
      }
      for (Class<?> paramType : constructors[0].getParameterTypes()) {
         ServiceEntry entry = ((ServiceMap) serviceMap).getEntry(paramType);
         if (entry == null) {
            throw new IllegalArgumentException(
                     "Dependent service " + paramType.getName() + " is not registered");
         }
         if (entry.getLevel().compareTo(level) < 0) {
            throw new IllegalArgumentException("Class " + provider.getName()
                     + " has a lower lifetime than one of its dependencies");
         }
      }
      this.provider = provider;
      this.serviceMap = serviceMap;
   }

   @Override
   @SuppressWarnings("unchecked")
   public T get() {
      Constructor<?> constructor = provider.getConstructors()[0];
      Object[] params = Arrays.stream(constructor.getParameterTypes()).map(serviceMap::get).toArray(
               Object[]::new);
      try {
         return (T) constructor.newInstance(params);
      } catch (InvocationTargetException e) {
         throw new LazyInstatiationException(e.getCause());
      } catch (Exception e) {
         throw new LazyInstatiationException(e);
      }
   }

}
