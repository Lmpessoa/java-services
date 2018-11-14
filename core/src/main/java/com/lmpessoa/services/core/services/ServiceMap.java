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
package com.lmpessoa.services.core.services;

import static com.lmpessoa.services.core.services.ReuseLevel.PER_REQUEST;
import static com.lmpessoa.services.core.services.ReuseLevel.SINGLETON;
import static com.lmpessoa.services.core.services.ReuseLevel.TRANSIENT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.lmpessoa.services.Internal;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.NonTraced;

@NonTraced
public final class ServiceMap implements IServiceMap {

   private final ThreadLocal<Map<Class<?>, Object>> threadPool = ThreadLocal.withInitial(HashMap::new);
   private final Map<Class<?>, ServiceEntry> entries = new HashMap<>();
   private final Map<Class<?>, Object> globalPool = new HashMap<>();

   @Internal
   public ServiceMap() {
      ClassUtils.checkInternalAccess();
   }

   @Override
   public <T> void useSingleton(Class<T> service, Supplier<T> supplier) {
      put(SINGLETON, service, Objects.requireNonNull(supplier));
   }

   @Override
   public <T, U extends T> void useSingleton(Class<T> service, U instance) {
      put(SINGLETON, service, null);
      globalPool.put(service, instance);
   }

   @Override
   public <T> void usePerRequest(Class<T> service, Supplier<T> supplier) {
      put(PER_REQUEST, service, Objects.requireNonNull(supplier));
   }

   @Override
   public <T> void useTransient(Class<T> service, Supplier<T> supplier) {
      put(TRANSIENT, service, Objects.requireNonNull(supplier));
   }

   @Override
   public boolean contains(Class<?> service) {
      return entries.containsKey(service);
   }

   @Override
   public Set<Class<?>> getServices() {
      return entries.keySet();
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T get(Class<T> clazz) {
      ClassUtils.checkInternalAccess();
      ServiceEntry entry = entries.get(clazz);
      if (entry == null) {
         String className = clazz.isArray() ? clazz.getComponentType().getName() + "[]" : clazz.getName();
         throw new NoSuchElementException("Service not found: " + className);
      }
      T value;
      Map<Class<?>, Object> levelPool = getPool(entry.getLevel());
      if (levelPool == null || !levelPool.containsKey(clazz)) {
         value = (T) entry.newInstance();
         if (levelPool != null) {
            levelPool.put(clazz, value);
         }
      } else {
         value = (T) levelPool.get(clazz);
      }
      return value;
   }

   @Override
   public Object invoke(Object obj, Method method) throws IllegalAccessException, InvocationTargetException {
      ClassUtils.checkInternalAccess();
      Object instance = obj instanceof Class<?> ? null : obj;
      if (Modifier.isStatic(method.getModifiers()) != (instance == null)) {
         throw new IllegalArgumentException("Mismatched static/instance method call");
      }
      List<Object> args = new ArrayList<>();
      for (Class<?> param : method.getParameterTypes()) {
         args.add(get(param));
      }
      method.setAccessible(true);
      return method.invoke(instance, args.toArray());
   }

   @Internal
   public <T> void putRequestValue(Class<T> service, T value) {
      ClassUtils.checkInternalAccess();
      threadPool.get().put(service, value);
   }

   @Internal
   public ServiceEntry getEntry(Class<?> service) {
      ClassUtils.checkInternalAccess();
      return entries.get(service);
   }

   private Map<Class<?>, Object> getPool(ReuseLevel level) {
      switch (level) {
         case SINGLETON:
            return globalPool;
         case PER_REQUEST:
            return threadPool.get();
         default:
            return null;
      }
   }

   private void put(ReuseLevel level, Class<?> service, Supplier<?> supplier) {
      if (entries.containsKey(Objects.requireNonNull(service))) {
         throw new IllegalArgumentException("Service " + service.getName() + " is already registered");
      }
      entries.put(service, new ServiceEntry(level, supplier));
   }
}
