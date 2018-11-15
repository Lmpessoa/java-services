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

import java.util.function.Supplier;

/**
 * A service registry is a registration service used to help locate other services by using a
 * service injection. Registration can be made into the following levels:
 * <ul>
 * <li><b>Singleton</b> services have one single instance respond for the service throughout the
 * entire lifetime of the application.</li>
 * <li><b>Per Request</b> services have a different instance respond for the service for each
 * request (or just like singleton but within each request).</li>
 * <li><b>Transient</b> services have one new instance every time the service is requested.</li>
 * </ul>
 */
public interface IServiceMap {

   // Singleton ----------

   /**
    * Adds the given class to the service map as a singleton service. The given class must be a
    * concrete class and is used both for service discovery and instantiation of the service responder.
    * <p>
    * Each request for a singleton service will return the same object regardless.
    * </p>
    *
    * @param service the class of the service to be registered.
    */
   default <T> void useSingleton(Class<T> service) {
      useSingleton(service, new LazyInitializer<>(service, SINGLETON, this));
   }

   /**
    * Adds the given class pair to the service map as a singleton service. The given service class is
    * used for service discovery while the provider class must be a concrete class and is used to
    * create the instance object that will respond to service requests.
    * <p>
    * Each request for a singleton service will return the same object regardless.
    * </p>
    *
    * @param service the class of the service for discovery.
    * @param provider the class that provides the service for the base class.
    */
   default <T> void useSingleton(Class<T> service, Class<? extends T> provider) {
      useSingleton(service, new LazyInitializer<T>(provider, SINGLETON, this));
   }

   /**
    * Adds the given provider function to the service map as singleton service. The given service class
    * is used for service discovery and the provider function is used to create the instance object
    * that will respond to service requests.
    * <p>
    * Each request for a singleton service will return the same object regardless.
    * </p>
    *
    * @param service the class of the service to be registered.
    * @param provider the function that creates objects that supply the service the service for the
    * base class.
    */
   <T> void useSingleton(Class<T> service, Supplier<T> provider);

   /**
    * Adds the given object instance to the service map as a singleton service. No other instances of
    * the responder will be created through this call.
    * <p>
    * Each request for a singleton service will return the same object regardless.
    * </p>
    *
    * @param service the class of the service to be registered.
    * @param instance the instance to be used to respond to service requests.
    */
   <T, U extends T> void useSingleton(Class<T> service, U instance);

   // Per Request ----------

   /**
    * Adds the given class to the service map as a per request service. The given class must be a
    * concrete class and is used both for service discovery and instantiation of the service responder.
    * <p>
    * As the name implies, each request for a per request service will return the same object within
    * each different request.
    * </p>
    *
    * @param service the class of the service to be registered.
    */
   default <T> void usePerRequest(Class<T> service) {
      usePerRequest(service, new LazyInitializer<>(service, PER_REQUEST, this));
   }

   /**
    * Adds the given class pair to the service map as a per request service. The given service class is
    * used for service discovery while the provider class must be a concrete class and is used to
    * create the instance object that will respond to service requests.
    * <p>
    * As the name implies, each request for a per request service will return the same object within
    * each different request.
    * </p>
    *
    * @param service the class of the service for discovery.
    * @param provider the class that provides the service for the base class.
    */
   default <T> void usePerRequest(Class<T> service, Class<? extends T> provider) {
      usePerRequest(service, new LazyInitializer<T>(provider, PER_REQUEST, this));
   }

   /**
    * Adds the given provider function to the service map as per request service. The given service
    * class is used for service discovery and the provider function is used to create the instance
    * object that will respond to service requests.
    * <p>
    * As the name implies, each request for a per request service will return the same object within
    * each different request.
    * </p>
    *
    * @param service the class of the service to be registered.
    * @param provider the function that creates objects that supply the service the service for the
    * base class.
    */
   <T> void usePerRequest(Class<T> service, Supplier<T> supplier);

   // Transient ----------

   /**
    * Adds the given class to the service map as a transient service. The given class must be a
    * concrete class and is used both for service discovery and instantiation of the service responder.
    * <p>
    * Each request for a transient object will return a new object.
    * </p>
    *
    * @param service the class of the service to be registered.
    */
   default <T> void useTransient(Class<T> service) {
      useTransient(service, new LazyInitializer<>(service, TRANSIENT, this));
   }

   /**
    * Adds the given class pair to the service map as a transient service. The given service class is
    * used for service discovery while the provider class must be a concrete class and is used to
    * create the instance object that will respond to service requests.
    * <p>
    * Each request for a transient object will return a new object.
    * </p>
    *
    * @param service the class of the service for discovery.
    * @param provider the class that provides the service for the base class.
    */
   default <T> void useTransient(Class<T> service, Class<? extends T> provider) {
      useTransient(service, new LazyInitializer<T>(provider, TRANSIENT, this));
   }

   /**
    * Adds the given provider function to the service map as transient service. The given service class
    * is used for service discovery and the provider function is used to create the instance object
    * that will respond to service requests.
    * <p>
    * Each request for a transient object will return a new object.
    * </p>
    *
    * @param service the class of the service to be registered.
    * @param provider the function that creates objects that supply the service the service for the
    * base class.
    */
   <T> void useTransient(Class<T> service, Supplier<T> supplier);
}
