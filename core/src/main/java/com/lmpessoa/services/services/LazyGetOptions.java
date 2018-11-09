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

import java.lang.reflect.Method;
import java.util.function.Supplier;

import com.lmpessoa.services.hosting.InternalServerError;

final class LazyGetOptions<T> implements Supplier<T> {

   private final ServiceMap serviceMap;
   private final Class<?> serviceClass;

   LazyGetOptions(Class<T> configClass, ServiceMap serviceMap, Class<?> serviceClass) {
      this.serviceMap = serviceMap;
      this.serviceClass = serviceClass;
   }

   @Override
   @SuppressWarnings("unchecked")
   public T get() {
      try {
         Object service = serviceMap.get(serviceClass);
         Method getOptions = service.getClass().getMethod("getOptions");
         return (T) getOptions.invoke(service);
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }

}
