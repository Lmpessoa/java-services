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

/**
 * Describe the configuration options for routes.
 *
 * <p>
 * Objects of this class are injected during the configuration phase of the application to allow it
 * to be configured before the application is running and resource classes are loaded.
 * </p>
 */
public interface IRouteOptions {

   /**
    * Adds an area to be associated with resources matching the given package.
    *
    * <p>
    * Routes are a form oF grouping resources at the root level of the application. Each route can
    * consist of one or two static parts which will be prepended to each resource route that matches
    * the package expression. Note that resources may be registered manually and those not matching
    * any other route will be added to the default root route.
    * </p>
    *
    * <p>
    * Areas also define which packages will be scanned for automatic registration by the
    * application. The empty route (an empty string) represents the default area and can be set to
    * define the expression of packages that will automatically be added to the root of the
    * application.
    * </p>
    *
    * @param areaPath the path of the area being registered.
    * @param packageExpr an expression to match the package to define if a class belongs to this
    *           area.
    */
   default void addArea(String areaPath, String packageExpr) {
      addArea(areaPath, packageExpr, "index");
   }

   /**
    * Adds an area to be associated with resource matching the given package.
    * <p>
    * Routes are a form oF grouping resources at the root level of the application. Each route can
    * consist of one or two static parts which will be prepended to each resource route that matches
    * the package expression. Note that resources may be registered manually and those not matching
    * any other route will be added to the default root route.
    * </p>
    *
    * <p>
    * Areas also define which packages will be scanned for automatic registration by the
    * application. The empty route (an empty string) represents the default area and can be set to
    * define the expression of packages that will automatically be added to the root of the
    * application.
    * </p>
    *
    * @param areaPath the path of the area being registered.
    * @param packageExpr an expression to match the package to define if a class belongs to this
    *           area.
    * @param defaultResource the name of the resource which will register as the default for this
    *           area (default is "index").
    */
   void addArea(String areaPath, String packageExpr, String defaultResource);
}
