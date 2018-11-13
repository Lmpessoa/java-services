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

/**
 * Describe the configuration options for the application.
 *
 * <p>
 * Objects of this class are injected during the configuration phase of the application to allow it
 * to be configured before the application is running and resource classes are loaded.
 * </p>
 */
public interface IApplicationOptions {

   /**
    * Sets the port to use to listen to requests for this application.
    *
    * <p>
    * Default port is <u>5617</u>.
    * </p>
    *
    * @param port the port to use for this application.
    */
   void usePort(int port);

   /**
    * Binds the application to the localhost.
    *
    * <p>
    * Binding the application to the localhost will make the application to respond only to requests on
    * the localhost. Request on other network interfaces will be ignored. Use this configuration when
    * the application is to be run behind a relay HTTP server.
    * </p>
    */
   default void bindToLocalhost() {
      bindToAddress("localhost");
   }

   /**
    * Binds the application to the given address.
    *
    * <p>
    * Binding the application to a given address will make the application to respond only to requests
    * on that address. If this method is given the address of '0.0.0.0' it will respond to requests
    * made in all network interfaces available on the machine. A different address will bind the
    * application to respond only to request from the associated network interface.
    * </p>
    *
    * <p>
    * Although this method exists, it is highly recommended it is not used to configure applications as
    * the address of network interfaces is likely to change. Instead either call only
    * {@link #bindToLocalhost()} or provide the desired IP address when starting the application.
    * </p>
    *
    * @param addr the address of the interface to bind the application to.
    */
   void bindToAddress(String addr);

   /**
    * Declares the application will accept requests made using XML.
    *
    * <p>
    * By default, applications only accept requests to be made using JSON. By calling this
    * method during the configuration phase of the application lifecycle will enable the application to
    * also accept requests made using XML content and respond with XML when requested.
    * </p>
    */
   void acceptXmlRequests();

   /**
    * Adds a class to the engine pipeline to handle requests and responses.
    *
    * <p>
    * Each handler can perform operations before and after the next handler. A handler may also decide
    * not to pass a request to the next, which is called short-circuiting the request pipeline.
    * Short-circuiting is often desirable because it avoids unnecessary work.
    * </p>
    *
    * <p>
    * The order in which handlers are added in the configuration phase also defines the order in which
    * they are invoked when handling requests, and the reverse order for the response. This ordering is
    * critical for security, performance, and functionality.
    * </p>
    *
    * @param handlerClass the class of the handler to add.
    */
   void addHandler(Class<?> handlerClass);

   /**
    * Limits the maximum number of jobs the application should execute in parallel.
    *
    * <p>
    * By default, an application will handle as many jobs as it is tasked with. In a development
    * environment this usually not an issue but may be in staging and production environments. This
    * method allows developers to limit how many concurrent jobs can be executed by the application.
    * </p>
    *
    * @param maxJobs the maximum number of jobs that the application should execute in parallel.
    */
   void limitConcurrentJobs(int maxJobs);
}
