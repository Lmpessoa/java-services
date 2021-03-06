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
package com.lmpessoa.services.hosting;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.xml.bind.annotation.XmlRootElement;

import com.lmpessoa.services.concurrent.IAsyncOptions;
import com.lmpessoa.services.internal.services.NoSingleMethodException;
import com.lmpessoa.services.routing.IRouteOptions;
import com.lmpessoa.services.security.IIdentityOptions;
import com.lmpessoa.services.security.ITokenManager;

/**
 * Enables the configuration of the behaviour of applications.
 *
 * <p>
 * Applications may use the methods exposed by this interface to configure how applications should
 * behave under various circumstances. These options are enabled only at development time, thus
 * representing behaviours that should not be modified after the application is deployed.
 * </p>
 */
public interface IApplicationOptions {

   /**
    * Registers the given class as a handler for requests received by the application.
    * <p>
    * Classes registered with this method must not be abstract, must have a public constructor
    * receiving a {@link NextResponder} as its only argument and must have a single method named
    * {@code invoke} using any number of registered service classes as arguments. Trying to add a
    * class that does not comply with this causes this method to throw an
    * {@code IllegalArgumentException}.
    * </p>
    * <p>
    * Handlers can invoke the next handler in the chain by calling {@link NextResponder#invoke()}.
    * The return value of this method is the value returned from invoking the next handler itself.
    * Thus, with it, a handler can decide whether the next handler must be called or not, modify the
    * value returned by it, or even handle thrown exceptions.</li>
    * </p>
    * <p>
    * Although not required, it is recommended that the {@code invoke} method implemented by the
    * handler returns at least an {@code Object} to return the result of the next handlers to the
    * previous handler (unless filtered or modified). If a handler returns {@code null} (or
    * {@code void}) the caller of the application will always receive a no content response
    * ({@code 204}).
    * </p>
    *
    * @param handlerClass the class to be
    * @throws IllegalArgumentException if the given class does not represent a concrete class, does
    *            not implement a constructor with a single {@code NextHandler} argument or does not
    *            implement a method named invoke.
    */
   void useResponder(Class<?> responderClass);

   /**
    * Enables an application to expand the capabilities of route registration.
    * <p>
    * Applications might require to be configured beyond the default settings in order to provide a
    * better organisation or configuration of its resources. Developers may use this method to
    * register how routes should behave beyond the default mode.
    * </p>
    * <p>
    * Note that calling this method is not mandatory during the configuration of the application if
    * there is no need to further expand the behaviour of routes. However, if this method is called,
    * its argument cannot be {@code null}.
    * </p>
    *
    * @param options a method used to configure the behaviour of routes in the application.
    */
   void useRoutesWith(Consumer<IRouteOptions> options);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given class must be a concrete class and is used both for service discovery and
    * instantiation of the service responder.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    */
   <T> void useService(Class<T> serviceClass);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given {@code service} class is used for service discovery while the {@code provided} class
    * must be a concrete class and is used to create the instance object that will respond to
    * service requests.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    * @param implementationClass the class from which service instances for the base class are
    *           created.
    */
   <T> void useService(Class<T> serviceClass, Class<? extends T> implementationClass);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given instance will be used to respond to any requests for this service. No other
    * instances of the responder will be created through this call. Only services with reuse level
    * {@link ALWAYS} can be registered with a single instance through this method.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    * @param instance the instance to be used to respond to service requests.
    */
   <T> void useService(Class<T> serviceClass, T instance);

   /**
    * Registers a service for the given class on the service map.
    * <p>
    * The given {@code service} class is used for service discovery and the {@code supplier}
    * function is used to create the instance object that will respond to service requests.
    * </p>
    *
    * @param serviceClass the class of the service to be registered.
    * @param supplier the function that supplies new instances of the service.
    */
   <T> void useServiceWith(Class<T> serviceClass, Supplier<T> supplier);

   /**
    * Enables the use of asynchronous requests using the default options.
    *
    * <p>
    * Note that by calling this method if a resource with the path {@code '/feedback'} exists it may
    * prevent that resource from ever being called.
    * </p>
    */
   default void useAsync() {
      useAsyncWith(null);
   }

   /**
    * Enables the use of asynchronous requests using the given options.
    * <p>
    * Developers may use the options consumer to configure the asynchronous service i.e. by defining
    * the feedback path to monitor asynchronous results or by changing the default behaviour of
    * {@code @Async} annotations.
    * </p>
    * <p>
    * Note that if the path used on the call to configure feedback path matches that of a registered
    * resource it may prevent that resource from ever being called.
    * </p>
    *
    * @param options a method used to further configure asynchronous services used by the
    *           application.
    */
   void useAsyncWith(Consumer<IAsyncOptions> options);

   /**
    * Enables the use of asynchronous requests using the given feedback path. Other asynchronous
    * properties will return their default values.
    * <p>
    * This path must be a valid path (starting and ending with {@code '/'}) but not a full URL. This
    * path will be appended to the current application's server host address to build the correct
    * full URL to poll an asynchronous result.
    * </p>
    * <p>
    * Note that if this path matches that of a registered resource it may prevent that resource from
    * ever being called.
    * </p>
    *
    * @param feedbackPath the path to be used to poll asynchronous execution results.
    */
   default void useAsyncWithFeedbackPath(String feedbackPath) {
      useAsyncWith(options -> options.useFeedbackPath(feedbackPath));
   }

   /**
    * Enables the application server to publish static files from the default resource path.
    * <p>
    * By default, applications will serve any files in the {@code '/static'} folder located at the
    * project's main resource folder (in a Maven project, that means the path must exist under
    * {@code 'src/main/resources'}).
    * </p>
    * <p>
    * Mime-types for the files served through this feature are given solely by the file's last
    * extension part (that means a file name ending in {@code '.tar.gz'} will only be typed based on
    * the {@code '.gz'} part). Additional mime-types used by an application can be provided by
    * adding them to a file named {@code 'mime.types'} at the root of the main resources folder of
    * the application project.
    * </p>
    * <p>
    * To use a folder at a different path than the default, call
    * {@link #useStaticFilesAtPath(String)} with the desired path instead.
    * </p>
    */
   default void useStaticFiles() {
      useStaticFilesAtPath("/static");
   }

   /**
    * Enables the application server to publish static files from the given resource path.
    * <p>
    * By default, applications will serve any files in the given folder located at the project's
    * main resource folder. The path to the folder must be relative to the main resources folder of
    * the project (in a Maven project, that means the given path must exist under
    * {@code 'src/main/resources'}).
    * </p>
    * <p>
    * Mime-types for the files served through this feature are given solely by the file's last
    * extension part (that means a file name ending in {@code '.tar.gz'} will only be typed based on
    * the {@code '.gz'} part). Additional mime-types used by an application can be provided by
    * adding them to a file named {@code 'mime.types'} at the root of the main resources folder of
    * the application project.
    * </p>
    * <p>
    * To use a folder at the default path, call {@link #useStaticFiles()} instead.
    * </p>
    *
    * @param staticPath the path to the folder the application will publish resources from.
    */
   void useStaticFilesAtPath(String staticPath);

   /**
    * Enables the use of identities in requests using the given identity provider.
    * <p>
    * An identity provider must identify the user in a request and return an identity that
    * represents that user and its capabilities. The provider instance must implement one of the two
    * methods responsible for returning an identity extracted from each request.
    * </p>
    * <p>
    * Developers deciding to implement {@link #getIdentity(String, String)} must evaluate the
    * {@code method} to check whether or not the authentication mechanism is to be supported while
    * the {@code token} argument contains the remaining arguments used to identify the user.
    * </p>
    * <p>
    * Developers may opt to implement {@link #getIdentity(HttpRequest)} instead. This method will
    * enable full access to the request and allow applications to retrieve information about the
    * user's identity from anywhere in the request.
    * </p>
    *
    * @param identityProvider the identity provider to use with requests.
    * @throws NoSingleMethodException if the identity provider class does not have exactly one
    *            constructor.
    */
   default void useIdentityWith(Class<? extends ITokenManager> tokenManager)
      throws NoSingleMethodException {
      useIdentityWith(tokenManager, null);
   }

   /**
    * Enables the use of identities in requests using the given identity provider.
    * <p>
    * An identity provider must identify the user in a request and return an identity that
    * represents that user and its capabilities. The provider instance must implement one of the two
    * methods responsible for returning an identity extracted from each request.
    * </p>
    * <p>
    * Developers deciding to implement {@link #getIdentity(String, String)} must evaluate the
    * {@code method} to check whether or not the authentication mechanism is to be supported while
    * the {@code token} argument contains the remaining arguments used to identify the user.
    * </p>
    * <p>
    * Developers may opt to implement {@link #getIdentity(HttpRequest)} instead. This method will
    * enable full access to the request and allow applications to retrieve information about the
    * user's identity from anywhere in the request.
    * </p>
    *
    * @param identityProvider the identity provider to use with requests.
    * @param options a method used to further configure identity services used by the application.
    * @throws NoSingleMethodException if the identity provider class does not have exactly one
    *            constructor.
    */
   void useIdentityWith(Class<? extends ITokenManager> tokenManager,
      Consumer<IIdentityOptions> options) throws NoSingleMethodException;

   /**
    * Enables support for health requests from the application.
    * <p>
    * In large environments, it is important to monitor applications to ensure they are running and
    * that resources the application depends on are always available. By enabling health,
    * applications provide a common path (i.e. {@code /health}) that monitoring applications can
    * query for the status of this application.
    * </p>
    *
    * @see IApplicationInfo
    */
   default void useHeath() {
      useHealthAtPath("/health");
   }

   /**
    * Enables support for health request from the application at the given path.
    * <p>
    * In large environments, it is important to monitor applications to ensure they are running and
    * that resources the application depends on are always available. By enabling health,
    * applications provide a common path (i.e. {@code /health}) that monitoring applications can
    * query for the status of this application.
    * </p>
    * <p>
    * Developers may use this method to provide a different path for the health information due to
    * whatever constraints they are presented with, for example, requiring to have another resource
    * exposed at the default path or preventing unwanted peeks at such information.
    * </p>
    *
    * @param healthPath the path that should return health information for the application.
    *
    * @see IApplicationInfo
    */
   void useHealthAtPath(String healthPath);

   /**
    * Enables the use of XML in requests/responses.
    *
    * <p>
    * Developers might need to enable XML support in applications for a number of reasons, from
    * compliance with legacy systems (that cannot benefit from JSON) to easy of usage in a specific
    * platform.
    * </p>
    *
    * <p>
    * Returned data from methods in applications that require the usage of XML requests/responses
    * must be carefully crafted. Since the engine relies on JAXB for XML conversions, many
    * adjustments (e.g. using annotations) may be required for that content to be read from or
    * produced adequately from an XML request/response. For example, JAXB requires that all content
    * classes are annotated with {@link XmlRootElement}.
    * </p>
    */
   void useXmlRequests();
}
