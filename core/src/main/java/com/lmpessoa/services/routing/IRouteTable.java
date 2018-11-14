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
package com.lmpessoa.services.routing;

import java.util.Arrays;
import java.util.Collection;

import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.services.IConfigurable;

/**
 * Represents a map of routes of the application.
 * <p>
 * A route table is only used to register the methods from resource classes to be used by the
 * application. Any method added to the route table can respond to a request received by the
 * application.
 * </p>
 */
public interface IRouteTable extends IConfigurable<IRouteOptions> {

   static Class<? extends IRouteTable> getServiceClass() {
      return RouteTable.class;
   }

   /**
    * Adds the methods of the given resource class to this route table. If any method provides a route
    * and HTTP method that is already registered, the new value is discarded.
    *
    * @param clazz the class to add to the route table.
    * @return a list of exceptions raised during this call.
    */
   Collection<Exception> put(Class<?> clazz);

   /**
    * Adds the methods of the given resource class to this route table under the given area. If any
    * method provides a route and HTTP method that is already registered, the new value is discarded.
    *
    * @param area the path of the area under which the resource will be located.
    * @param clazz the class to add to the route table.
    * @return a list of exceptions raised during this call.
    */
   default Collection<Exception> put(String area, Class<?> clazz) {
      return putAll(area, Arrays.asList(clazz));
   }

   /**
    * Adds the methods of all the given resource classes to this route table. If any method provides a
    * route and HTTP method that is already registered, the new value is discarded.
    *
    * @param classes the collection of classes to add to the route table.
    * @return a list of exceptions raised during this call.
    */
   Collection<Exception> putAll(Collection<Class<?>> classes);

   /**
    * Adds the methods of all the given resource class to this route table under the given area. If any
    * method provides a route and HTTP method that is already registered, the new value is discarded.
    *
    * @param area the path of the area under which the resource will be located.
    * @param classes the collection of classes to add to the route table.
    * @return a list of exceptions raised during this call.
    */
   Collection<Exception> putAll(String area, Collection<Class<?>> classes);

   /**
    * Returns the name of the area classes in the given package name should belong to.
    *
    * @param packageName the name of the package to check.
    * @return the name of the area classes in the given package name should belong to, or
    * <code>null</code> if no area captures classes in the given package name.
    */
   String findArea(String packageName);

   MatchedRoute matches(HttpRequest request);
}
