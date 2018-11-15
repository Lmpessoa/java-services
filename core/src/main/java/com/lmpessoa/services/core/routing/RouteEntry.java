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
package com.lmpessoa.services.core.routing;

import java.lang.reflect.Method;

public final class RouteEntry {

   private final Method duplicateMethod;
   private final Class<?> sourceType;
   private final Method sourceMethod;
   private final Exception error;
   private final String route;

   public RouteEntry(Method sourceMethod, String route) {
      this(sourceMethod, sourceMethod.getDeclaringClass(), route, null, null);
   }

   public RouteEntry(Method sourceMethod, String route, Method duplicateMethod) {
      this(sourceMethod, sourceMethod.getDeclaringClass(), route, duplicateMethod, null);
   }

   public RouteEntry(Method sourceMethod, Exception error) {
      this(sourceMethod, sourceMethod.getDeclaringClass(), null, null, error);
   }

   public RouteEntry(Class<?> sourceType, Exception error) {
      this(null, sourceType, null, null, error);
   }

   public String getRoute() {
      return route;
   }

   public Method getMethod() {
      return sourceMethod;
   }

   public Class<?> getType() {
      return sourceType;
   }

   public Method getDuplicateOf() {
      return duplicateMethod;
   }

   public Exception getError() {
      return error;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof RouteEntry)) {
         return false;
      }
      RouteEntry other = (RouteEntry) obj;
      return duplicateMethod == other.duplicateMethod && sourceType == other.sourceType
               && sourceMethod == other.sourceMethod && error == other.error && route == null
                        ? other.route == null
                        : route.equals(other.route);
   }

   @Override
   public int hashCode() {
      return super.hashCode();
   }

   private RouteEntry(Method sourceMethod, Class<?> sourceType, String route,
      Method duplicateMethod, Exception error) {
      this.duplicateMethod = duplicateMethod;
      this.sourceMethod = sourceMethod;
      this.sourceType = sourceType;
      this.route = route;
      this.error = error;
   }
}
