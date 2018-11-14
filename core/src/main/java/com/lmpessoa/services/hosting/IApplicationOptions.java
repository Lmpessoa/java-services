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
}
