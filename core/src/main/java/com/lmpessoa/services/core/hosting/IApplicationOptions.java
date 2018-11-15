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
package com.lmpessoa.services.core.hosting;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.core.security.IIdentityOptions;
import com.lmpessoa.services.core.security.IIdentityProvider;
import com.lmpessoa.services.core.services.Reuse;
import com.lmpessoa.services.core.services.Service;

/**
 * Provides an interface to configure the behaviour of applications.
 */
@Service(Reuse.ALWAYS)
public interface IApplicationOptions {

   /**
    * Registers the given class as a handler for requests received by the application.
    * <p>
    * Classes registered with this method must not be abstract, must have a public constructor
    * receiving a {@link NextResponder} as its only argument and must have a single method named
    * <code>invoke</code> using any number of registered service classes as arguments. Trying to add a
    * class that does not comply with this causes this method to throw an
    * <code>IllegalArgumentException</code>.
    * </p>
    * <p>
    * Handlers can invoke the next handler in the chain by calling {@link NextResponder#invoke()}. The
    * return value of this method is the value returned from invoking the next handler itself. Thus,
    * with it, a handler can decide whether the next handler must be called or not, modify the value
    * returned by it, or even handle thrown exceptions.</li>
    * </p>
    * <p>
    * Although not required, it is recommended that the <code>invoke</code> method implemented by the
    * handler returns at least an <code>Object</code> to return the result of the next handlers to the
    * previous handler (unless filtered or modified). If a handler returns <code>null</code> (or
    * <code>void</code>) the caller of the application will always receive a no content response
    * (<code>204</code>).
    * </p>
    *
    * @param handlerClass the class to be
    * @throws IllegalArgumentException if the given class does not represent a concrete class, does not
    * implement a constructor with a single <code>NextHandler</code> argument or does not implement a
    * method named invoke.
    */
   void useResponder(Class<?> responderClass);

   /**
    * Enables an application to expand the capabilities of route registration.
    * <p>
    * Applications might require to be configured beyond the default settings in order to provide a
    * better organisation or configuration of its resources. Developers may use this method to register
    * how routes should behave beyond the default mode.
    * </p>
    * <p>
    * Note that calling this method is not mandatory during the configuration of the application if
    * there is no need to further expand the behaviour of routes. However, if this method is called,
    * its argument cannot be {@code null}.
    * </p>
    *
    * @param options a method used to configure the behaviour of routes in the application.
    */
   void useRoutes(Consumer<IRouteOptions> options);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given class must be a concrete class and is used both for service discovery and instantiation
    * of the service responder.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    */
   <T> void useService(Class<T> serviceClass);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given {@code service} class is used for service discovery while the {@code provided} class
    * must be a concrete class and is used to create the instance object that will respond to service
    * requests.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    * @param implementationClass the class from which service instances for the base class are created.
    */
   <T> void useService(Class<T> serviceClass, Class<? extends T> implementationClass);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given {@code service} class is used for service discovery and the {@code supplier} function
    * is used to create the instance object that will respond to service requests.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    * @param supplier the function that supplies new instances of the service.
    */
   <T> void useService(Class<T> serviceClass, Supplier<T> supplier);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given instance will be used to respond to any requests for this service. No other instances
    * of the responder will be created through this call. Only services with reuse level {@link ALWAYS}
    * can be registered with a single instance through this method.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    * @param instance the instance to be used to respond to service requests.
    */
   <T> void useService(Class<T> serviceClass, T instance);

   /**
    * Enables the use of asynchronous requests using the default feedback path to return information
    * about running requests.
    * <p>
    * Note that by calling this method if a resource with the path {@code '/feedback'} exists it may
    * prevent that resource from ever being called.
    * </p>
    */
   void userAsync();

   /**
    * Enables the use of asynchronous requests using the given feedback path to return information
    * about running requests.
    * <p>
    * This path must be a valid path (starting and ending with {@code '/'}) but not a full URL. This
    * path will be appended to the current application's server host address to build the correct full
    * URL to poll an asynchronous result.
    * </p>
    * <p>
    * Note that if this path matches that of a registered resource it may prevent that resource from
    * ever being called.
    * </p>
    *
    * @param feedbackPath the path to be used to poll asynchronous execution results.
    */
   void useAsyncWithFeedbackPath(String feedbackPath);

   /**
    * Enables the use of identities in requests using the given identity provider.
    * <p>
    * An identity provider must identify the user in a request and return an identity that represents
    * that user and its capabilities. The provider instance must implement one of the two methods
    * responsible for returning an identity extracted from each request.
    * </p>
    * <p>
    * Developers deciding to implement {@link #getIdentity(String, String)} must evaluate the
    * {@code method} to check whether or not the authentication mechanism is to be supported while the
    * {@code token} argument contains the remaining arguments used to identify the user.
    * </p>
    * <p>
    * Developers may opt to implement {@link #getIdentity(HttpRequest)} instead. This method will
    * enable full access to the request and allow applications to retrieve information about the user's
    * identity from anywhere in the request.
    * </p>
    *
    * @param identityProvider the identity provider to use with requests.
    */
   void useIdentity(IIdentityProvider identityProvider);

   /**
    * Enables the use of identities in requests using the given identity provider.
    * <p>
    * An identity provider must identify the user in a request and return an identity that represents
    * that user and its capabilities. The provider instance must implement one of the two methods
    * responsible for returning an identity extracted from each request.
    * </p>
    * <p>
    * Developers deciding to implement {@link #getIdentity(String, String)} must evaluate the
    * {@code method} to check whether or not the authentication mechanism is to be supported while the
    * {@code token} argument contains the remaining arguments used to identify the user.
    * </p>
    * <p>
    * Developers may opt to implement {@link #getIdentity(HttpRequest)} instead. This method will
    * enable full access to the request and allow applications to retrieve information about the user's
    * identity from anywhere in the request.
    * </p>
    *
    * @param identityProvider the identity provider to use with requests.
    * @param options a method used to further configure identity services used by the application.
    */
   void useIdentity(IIdentityProvider identityProvider, Consumer<IIdentityOptions> options);
}
