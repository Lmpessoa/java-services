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

/**
 * Provides an interface to configure application options.
 */
public interface IApplicationOptions {

   /**
    * Registers the given class as a handler for requests received by the application.
    * <p>
    * Classes registered with this method must not be abstract, must have a public constructor
    * receiving a {@link NextHandler} as its only argument and must have a single method named
    * <code>invoke</code> using any number of registered service classes as arguments. Trying to add a
    * class that does not comply with this causes this method to throw an
    * <code>IllegalArgumentException</code>.
    * </p>
    *
    * <p>
    * Handlers can invoke the next handler in the chain by calling {@link NextHandler#invoke()}. The
    * return value of this method is the value returned from invoking the next handler itself. Thus,
    * with it, a handler can decide whether the next handler must be called or not, modify the value
    * returned by it, or even handle thrown exceptions.</li>
    * </p>
    *
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
   void useHandler(Class<?> handlerClass);
}
