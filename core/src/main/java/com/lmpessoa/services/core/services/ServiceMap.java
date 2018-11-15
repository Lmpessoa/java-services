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

import static com.lmpessoa.services.core.services.Reuse.ALWAYS;

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

/**
 * A {@code ServiceMap} is a registration service used to locate instances required for the
 * execution of certain methods or initialisation of those instances.
 *
 * <p>
 * Service maps provide a centralised manager for construction and reuse of instances of registered
 * types in various levels of reuse, meaning it will coordinate and provide instances of the
 * registered types reusing created instances as defined by the service type.
 * </p>
 *
 * <p>
 * After registration, a {@code ServiceMap} can be used to obtain instances of any service by its
 * type or used to invoke other methods which require only registered services.
 * </p>
 *
 * @see Service
 * @see Reuse
 */
public final class ServiceMap implements IServiceMap {

   private final ThreadLocal<Map<Class<?>, Object>> threadPool = ThreadLocal.withInitial(HashMap::new);
   private final Map<Class<?>, ServiceEntry> entries = new HashMap<>();
   private final Map<Class<?>, Object> globalPool = new HashMap<>();

   @Override
   public <T> void put(Class<T> service, Supplier<T> supplier) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException("Service does not specify reuse level");
      }
      put(ann.value(), service, Objects.requireNonNull(supplier));

   }

   @Override
   public <T> void put(Class<T> service, T instance) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException("Service does not specify reuse level");
      }
      if (ann.value() != Reuse.ALWAYS) {
         throw new IllegalArgumentException("Service instances can only be used if reuse is ALWAYS");
      }
      put(ALWAYS, service, null);
      globalPool.put(service, instance);

   }

   /**
    * Returns whether this {@code ServiceMap} can handle the given service type.
    *
    * @param service the class to test if a service is registered for.
    * @return {@code true} if the {@code ServiceMap} can handle the given service type, {@code false}
    * otherwise.
    */
   public boolean contains(Class<?> service) {
      return entries.containsKey(service);
   }

   /**
    * Returns a list of service types this {@code ServiceMap} can handle.
    *
    * @return a list of service types this {@code ServiceMap} can handle.
    */
   public Set<Class<?>> getServices() {
      return entries.keySet();
   }

   /**
    * Returns an instance of the given service type.
    *
    * <p>
    * This method will handle creating any intermediary services required to fullfil the required
    * service and returning the appropriate instance for the context (see {@link Reuse}).
    * </p>
    *
    * @param clazz the type of the desired service.
    * @return an instance of the given service type.
    * @throws NoSuchElementException if the {@code ServiceMap} cannot handle the given service type.
    */
   @SuppressWarnings("unchecked")
   public <T> T get(Class<T> clazz) {
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

   /**
    * Invokes the method with the given name on the given object and returns any result returned by
    * that method.
    *
    * <p>
    * This method will resolve the method to be called and all of its arguments must be registered
    * services in this same {@code ServiceMap}.
    * </p>
    *
    * <p>
    * For this method to work, however, it is required the class of the object to bear exactly one
    * method with the given name, otherwise a {@see NoSingleMethodException} will be thrown.
    * </p>
    *
    * @param obj the object to invoke the method with.
    * @param methodName the name of the method to be invoked.
    * @return
    * @throws NoSingleMethodException if the class of the object do not have exactly one method with
    * the given name.
    * @throws IllegalAccessException if this Method object is enforcing Java language access control
    * and the underlying method is inaccessible.
    * @throws InvocationTargetException if the underlying method throws an exception.
    */
   public Object invoke(Object obj, String methodName)
      throws NoSingleMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
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

   /**
    * Invokes the given method or constructor and returns any result returned by that call.
    *
    * <p>
    * This method will resolve all of the arguments required to execute the method or constructor as
    * long as they are registered services in this same {@code ServiceMap}.
    * </p>
    *
    * @param obj the object to invoke the method with. Can be null if the executable is a constructor.
    * @param exec the method or constructor to be invoked.
    * @return
    * @throws IllegalAccessException if this Method object is enforcing Java language access control
    * and the underlying method or constructor is inaccessible.
    * @throws InvocationTargetException if the underlying executable throws an exception.
    * @throws InstantiationException if the executable represents a constructor from an abstract class.
    * @throws NullPointerException if the a method or constructor to be executed was not provided or if
    * specified object is null and the method is an instance method.
    */
   public Object invoke(Object obj, Executable exec)
      throws IllegalAccessException, InvocationTargetException, InstantiationException {
      Objects.requireNonNull(exec);
      if (!Modifier.isStatic(exec.getModifiers()) && obj != null
               && !exec.getDeclaringClass().isAssignableFrom(obj.getClass())) {
         throw new IllegalArgumentException("Mismatched static/instance method call");
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

   /**
    * Adds the given value to the current {@code REQUEST} pool.
    *
    * <p>
    * Sometimes it is not possible to provide an actual supplier for request level services, mostly
    * because these values are being produced during the initialisation of the request. This method
    * allows those produced values to be set on the request level before any automatic value is
    * produced by the engine.
    * </p>
    *
    * @param service the service to set the value to.
    * @param value the value to be used with the service for the current request.
    * @throws IllegalArgumentException if the service is not registered in this {@code ServiceMap} or
    * if it is not defined with {@link Reuse#REQUEST}.
    * @throws IllegalStateException if a value has already been produced for the given service.
    */
   public <T> void putRequestValue(Class<T> service, T value) {
      ServiceEntry entry = entries.get(service);
      if (entry == null) {
         throw new IllegalArgumentException("Unknown service: " + service.getName());
      }
      if (entry.getLevel() != Reuse.REQUEST) {
         throw new IllegalArgumentException("Service is not registered per request");
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
         throw new IllegalArgumentException("Service " + service.getName() + " is already registered");
      }
      entries.put(service, new ServiceEntry(level, supplier));
   }
}
