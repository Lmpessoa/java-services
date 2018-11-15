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
package com.lmpessoa.services.core.routing;

import java.lang.reflect.Method;

import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.MatchedRoute;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.logging.ILogger;

public final class RouteTableBridge {

   public static IRouteTable get(ServiceMap serviceMap, ILogger log) throws NoSuchMethodException {
      return new RouteTable(serviceMap, log);
   }

   public static RouteMatch match(IRouteTable routes, HttpRequest request) {
      return ((RouteTable) routes).matches(request);
   }

   public static boolean isMatchedRoute(RouteMatch route) {
      return route instanceof MatchedRoute;
   }

   public static Method getMatchedRouteMethod(RouteMatch route) {
      if (isMatchedRoute(route)) {
         return ((MatchedRoute) route).getMethod();
      }
      return null;
   }

   public static Object[] getMatchedRouteMethodArgs(RouteMatch route) {
      if (isMatchedRoute(route)) {
         return ((MatchedRoute) route).getMethodArgs();
      }
      return null;
   }
}
