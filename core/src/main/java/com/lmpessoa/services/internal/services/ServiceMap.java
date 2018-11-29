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

import static com.lmpessoa.services.services.Reuse.ALWAYS;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.services.Reuse;
import com.lmpessoa.services.services.Service;

public final class ServiceMap {

   private final ThreadLocal<Map<Class<?>, Object>> threadPool = ThreadLocal
            .withInitial(HashMap::new);
   private final Map<Class<?>, ServiceEntry> entries = new HashMap<>();
   private final Map<Class<?>, Object> globalPool = new HashMap<>();

   public <T> void put(Class<T> service) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException(
                  CoreMessage.SERVICE_MISSING_REUSE.with(service.getName()));
      }
      putSupplier(service, new LazyInitializer<>(service, ann.reuse(), this));
   }

   public <T> void put(Class<T> service, Class<? extends T> provided) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException(
                  CoreMessage.SERVICE_MISSING_REUSE.with(service.getName()));
      }
      putSupplier(service, new LazyInitializer<>(provided, ann.reuse(), this));
   }

   public <T> void put(Class<T> service, T instance) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException(
                  CoreMessage.SERVICE_MISSING_REUSE.with(service.getName()));
      }
      if (ann.reuse() != Reuse.ALWAYS) {
         throw new IllegalArgumentException(CoreMessage.SERVICE_SINGLETON.get());
      }
      put(ALWAYS, service, null);
      globalPool.put(service, instance);

   }

   public <T> void putSupplier(Class<T> service, Supplier<T> supplier) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException(
                  CoreMessage.SERVICE_MISSING_REUSE.with(service.getName()));
      }
      put(ann.reuse(), service, Objects.requireNonNull(supplier));

   }

   public boolean contains(Class<?> service) {
      return entries.containsKey(service);
   }

   public Set<Class<?>> getServices() {
      return entries.keySet();
   }

   @SuppressWarnings("unchecked")
   public <T> T get(Class<T> clazz) {
      ServiceEntry entry = entries.get(clazz);
      if (entry == null) {
         String className = clazz.isArray() ? clazz.getComponentType().getName() + "[]"
                  : clazz.getName();
         throw new NoSuchElementException(CoreMessage.SERVICE_NOT_FOUND.with(className));
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

   public Object invoke(Object obj, String methodName) throws NoSingleMethodException,
      IllegalAccessException, InvocationTargetException, InstantiationException {
      Class<?> clazz = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
      Method[] methods = Arrays.stream(clazz.getMethods())
               .filter(m -> methodName.equals(m.getName()))
               .toArray(Method[]::new);
      if (methods.length != 1) {
         throw new NoSingleMethodException(
                  CoreMessage.TOO_MANY_METHODS.with(clazz.getName(), methodName, methods.length));
      }
      return invoke(obj, methods[0]);
   }

   public Object invoke(Object obj, Executable exec)
      throws IllegalAccessException, InvocationTargetException, InstantiationException {
      Objects.requireNonNull(exec);
      if (!Modifier.isStatic(exec.getModifiers()) && obj != null
               && !exec.getDeclaringClass().isInstance(obj)) {
         throw new IllegalArgumentException(CoreMessage.MISMATCHED_CALL.get());
      }
      List<Object> args = new ArrayList<>();
      for (Class<?> param : exec.getParameterTypes()) {
         args.add(get(param));
      }
      exec.setAccessible(true);
      if (exec instanceof Constructor<?>) {
         return ((Constructor<?>) exec).newInstance(args.toArray());
      } else if (exec instanceof Method) {
         return ((Method) exec).invoke(obj, args.toArray());
      }
      throw new UnsupportedOperationException();
   }

   public <T> void putRequestValue(Class<T> service, T value) {
      ServiceEntry entry = entries.get(service);
      if (entry == null) {
         throw new IllegalArgumentException(CoreMessage.SERVICE_NOT_FOUND.with(service.getName()));
      }
      if (entry.getLevel() != Reuse.REQUEST) {
         throw new IllegalArgumentException(CoreMessage.SERVICE_NOT_PER_REQUEST.get());
      }
      threadPool.get().put(service, value);
   }

   ServiceEntry getEntry(Class<?> service) {
      return entries.get(service);
   }

   private Map<Class<?>, Object> getPool(Reuse level) {
      switch (level) {
         case ALWAYS:
            return globalPool;
         case REQUEST:
            return threadPool.get();
         default:
            return null;
      }
   }

   private void put(Reuse level, Class<?> service, Supplier<?> supplier) {
      if (entries.containsKey(Objects.requireNonNull(service))) {
         throw new IllegalArgumentException(CoreMessage.SERVICE_REGISTERED.with(service.getName()));
      }
      entries.put(service, new ServiceEntry(level, supplier));
   }
}
