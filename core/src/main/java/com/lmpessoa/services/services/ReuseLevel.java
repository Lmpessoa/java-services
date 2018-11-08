/*
 * A light and easy engine for developing web APIs and microservices.
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

import java.util.HashMap;
import java.util.Map;

enum ReuseLevel implements IServiceLevelPool {
   TRANSIENT {

      @Override
      public Map<Class<?>, Object> getPool(IServiceMap map) {
         return new HashMap<>();
      }
   },
   PER_REQUEST {

      @Override
      public Map<Class<?>, Object> getPool(IServiceMap map) {
         final Thread thread = Thread.currentThread();
         if (!(thread instanceof IServicePoolProvider)) {
            throw new IllegalStateException("Cannot access object pool for this service map");
         }
         return ((IServicePoolProvider) thread).getPool();
      }
   },
   SINGLETON {

      @Override
      public Map<Class<?>, Object> getPool(IServiceMap map) {
         if (!(map instanceof IServicePoolProvider)) {
            throw new IllegalStateException("Cannot access object pool for this service map");
         }
         return ((IServicePoolProvider) map).getPool();
      }
   };
}
