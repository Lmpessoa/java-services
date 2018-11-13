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
package com.lmpessoa.services.services;

import static com.lmpessoa.services.services.ReuseLevel.PER_REQUEST;
import static com.lmpessoa.services.services.ReuseLevel.SINGLETON;
import static com.lmpessoa.services.services.ReuseLevel.TRANSIENT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

final class ServiceMap implements IServiceMap, IServicePoolProvider {

   private final Map<Class<?>, ServiceEntry> entries = new HashMap<>();
   private final Map<Class<?>, Object> pool = new HashMap<>();

   @Override
   public <T> void putSingleton(Class<T> service, Supplier<T> supplier) {
      put(SINGLETON, service, Objects.requireNonNull(supplier));
   }

   @Override
   public <T> void putSingleton(Class<T> service, T instance) {
      put(SINGLETON, service, null);
      pool.put(service, instance);
   }

   @Override
   public <T> void putPerRequest(Class<T> service, Supplier<T> supplier) {
      put(PER_REQUEST, service, Objects.requireNonNull(supplier));
   }

   @Override
   public <T> void putTransient(Class<T> service, Supplier<T> supplier) {
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
   public Map<Class<?>, Object> getPool() {
      return pool;
   }

   @SuppressWarnings("unchecked")
   public <T> T get(Class<T> clazz) {
      ServiceEntry entry = entries.get(clazz);
      if (entry == null) {
         String className = clazz.isArray() ? clazz.getComponentType().getName() + "[]" : clazz.getName();
         throw new NoSuchElementException("Service not found: " + className);
      }
      Map<Class<?>, Object> levelPool = entry.getLevel().getPool(this);
      if (!levelPool.containsKey(clazz)) {
         Object obj = entry.newInstance();
         if (!clazz.isAssignableFrom(obj.getClass())) {
            throw new ClassCastException("Cannot convert " + obj.getClass().getName() + " to " + clazz.getName());
         }
         levelPool.put(clazz, obj);
      }
      return (T) levelPool.get(clazz);
   }

   Object invoke(Object obj, String methodName)
      throws NoSingleMethodException, IllegalAccessException, InvocationTargetException {
      Class<?> clazz = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
      Method[] methods = Arrays.stream(clazz.getMethods()).filter(m -> methodName.equals(m.getName())).toArray(
               Method[]::new);
      if (methods.length != 1) {
         throw new NoSingleMethodException(
                  "Class " + clazz.getName() + " must have exactly one method named '" + methodName + "'",
                  methods.length);
      }
      return invoke(obj, methods[0]);
   }

   Object invoke(Object obj, Method method) throws IllegalAccessException, InvocationTargetException {
      Object instance = obj instanceof Class<?> ? null : obj;
      if (Modifier.isStatic(method.getModifiers()) != (instance == null)) {
         throw new IllegalArgumentException("Mismatched static/instance method call");
      }
      Object[] args = Arrays.stream(method.getParameterTypes()).map(this::get).toArray();
      method.setAccessible(true);
      return method.invoke(instance, args);
   }

   @Override
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public IServiceMap getConfigMap() {
      ServiceMap result = new ServiceMap();
      entries.entrySet()
               .stream() //
               .filter(e -> ReuseLevel.SINGLETON.equals(e.getValue().getLevel())) //
               .map(Entry::getKey) //
               .filter(c -> c.isAnnotationPresent(ConfiguresWith.class))
               .forEach(c -> {
                  ConfiguresWith config = c.getAnnotation(ConfiguresWith.class);
                  result.putSingleton(config.value(), new LazyGetOptions(config.value(), this, c));
               });
      return result;
   }

   ServiceEntry getEntry(Class<?> clazz) {
      return entries.get(clazz);
   }

   private void put(ReuseLevel level, Class<?> service, Supplier<?> supplier) {
      if (entries.containsKey(Objects.requireNonNull(service))) {
         throw new IllegalArgumentException("Service " + service.getName() + " is already registered");
      }
      entries.put(service, new ServiceEntry(level, supplier));
   }
}
