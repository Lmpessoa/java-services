/*
 * Copyright (c) 2018 Leonardo Pessoa
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
package com.lmpessoa.services.routing;

import static com.lmpessoa.services.services.Reuse.REQUEST;

import java.lang.reflect.Method;

import com.lmpessoa.services.services.Service;

/**
 * A {@code RouteMatch} represents a set of information about a matched request route to enable the
 * resolved route method to be executed.
 *
 * <p>
 * Instances of this class are produced by matching HTTP request information with the route table of
 * the application. Thus, instances of this class represent an abstraction of the request from the
 * HTTP protocol.
 * </p>
 */
@Service(reuse = REQUEST)
public interface RouteMatch {

   /**
    * Returns the resource class that contains the method matched by this route.
    *
    * @return the resource class that contains the method matched by this route.
    */
   default Class<?> getResourceClass() {
      return null;
   }

   /**
    * Returns the method of the resource class matched by this route.
    *
    * @return the method of the resource class matched by this route.
    */
   default Method getMethod() {
      return null;
   }

   default Object getContentObject() {
      return null;
   }

   /**
    * Calls the resolved method.
    *
    * <p>
    * This method does not expect arguments provided by the developer since all the expected
    * arguments gathered from the HTTP request are internally known to this class.
    * </p>
    *
    * @return a object result returned by the called method or {@code null} if nothing was returned.
    */
   Object invoke();
}
