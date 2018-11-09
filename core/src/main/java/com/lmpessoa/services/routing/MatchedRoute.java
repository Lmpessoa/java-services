/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.lmpessoa.services.core.HttpException;
import com.lmpessoa.services.core.InternalServerError;

/**
 * A <code>MatchedRoute</code> represents a set of information about a matched request route to
 * enable the resolved route method to be executed.
 *
 * <p>
 * Instances of this class are produced by matching HTTP request information with the route table of
 * the application. Thus, instances of this class represent an abstraction of the request from th
 * HTTP protocol.
 * </p>
 */
public final class MatchedRoute {

   private final Class<?> resourceClass;
   private final Object[] constructorArgs;
   private final Method method;
   private final Object[] methodArgs;

   MatchedRoute(MethodEntry entry, Object[] args) {
      this.resourceClass = entry.getResourceClass();
      this.constructorArgs = Arrays.copyOfRange(args, 0, entry.getResourceArgumentCount());
      this.method = entry.getMethod();
      this.methodArgs = Arrays.copyOfRange(args, entry.getResourceArgumentCount(), args.length);
   }

   /**
    * Returns the class of the resource matched by this route.
    *
    * @return the class of the resource matched by this route.
    */
   public Class<?> getResourceClass() {
      return resourceClass;
   }

   Object[] getConstructorArgs() {
      return constructorArgs;
   }

   /**
    * Returns the method of the resource class matched by this route.
    *
    * @return the method of the resource class matched by this route.
    */
   public Method getMethod() {
      return method;
   }

   Object[] getMethodArgs() {
      return methodArgs;
   }

   /**
    * Calls the resolved method.
    *
    * <p>
    * This method does not expect arguments provided by the developer since all the expected
    * arguments gathered from the HTTP request are internally known to this class.
    * </p>
    *
    * @return a object result returned by the called method or <code>null</code> if nothing was
    * returned.
    */
   public Object invoke() {
      try {
         Constructor<?> constructor = resourceClass.getConstructors()[0];
         Object resource = constructor.newInstance(constructorArgs);
         return method.invoke(resource, methodArgs);
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof HttpException) {
            throw (HttpException) e.getCause();
         }
         if (e.getCause() instanceof InternalServerError) {
            throw (InternalServerError) e.getCause();
         }
         throw new InternalServerError(e.getCause());
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }
}
