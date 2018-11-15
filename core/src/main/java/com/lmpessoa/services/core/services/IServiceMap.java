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

import java.util.function.Supplier;

/**
 * Represents the registration part of a {@link ServiceMap} within the engine.
 *
 * <p>
 * In dependency injection, it is not interesting that developed code has access to means of
 * retrieving objects of registered services. However, new services may be freely registered by the
 * application throughout its lifetime, perhaps due to extensions (plug-ins) or optional services
 * required due to additional conditions.
 * </p>
 *
 * <p>
 * It is important to note only that once a service is registered on a {@code ServiceMap} it cannot
 * be replaced or unregistered.
 * </p>
 */
@Service(Reuse.ALWAYS)
public interface IServiceMap {

   /**
    * Registers a service for the given class on the service map.
    *
    * <p>
    * The given class must be a concrete class and is used both for service discovery and instantiation
    * of the service responder.
    * </p>
    *
    * @param service the class of the service to be registered.
    */

   default <T> void put(Class<T> service) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException("Service does not specify reuse level");
      }
      put(service, new LazyInitializer<>(service, ann.value(), this));
   }

   /**
    * Registers a service for the given class on the service map.
    *
    * <p>
    * The given {@code service} class is used for service discovery while the {@code provided} class
    * must be a concrete class and is used to create the instance object that will respond to service
    * requests.
    * </p>
    *
    * @param service the class of the service to be registered.
    * @param provided the class from which service instances for the base class are created.
    */
   default <T> void put(Class<T> service, Class<? extends T> provided) {
      Service ann = service.getAnnotation(Service.class);
      if (ann == null) {
         throw new IllegalArgumentException("Service does not specify reuse level");
      }
      put(service, new LazyInitializer<>(provided, ann.value(), this));
   }

   /**
    * Registers a service for the given class on the service map.
    *
    * <p>
    * The given {@code service} class is used for service discovery and the {@code supplier} function
    * is used to create the instance object that will respond to service requests.
    * </p>
    *
    * @param service the class of the service to be registered.
    * @param supplier the function that supplies new instances of the service.
    */

   <T> void put(Class<T> service, Supplier<T> supplier);

   /**
    * Registers a service for the given class on the service map.
    *
    * <p>
    * The given instance will be used to respond to any requests for this service. No other instances
    * of the responder will be created through this call. Only services with reuse level {@link ALWAYS}
    * can be registered with a single instance through this method.
    * </p>
    *
    * @param service the class of the service to be registered.
    * @param instance the instance to be used to respond to service requests.
    */
   <T> void put(Class<T> service, T instance);
}
