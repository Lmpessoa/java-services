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
package com.lmpessoa.services.core.internal.routing;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.lmpessoa.services.core.hosting.BadRequestException;
import com.lmpessoa.services.core.hosting.InternalServerError;
import com.lmpessoa.services.core.internal.hosting.HttpException;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.validating.ErrorSet;
import com.lmpessoa.services.core.validating.IValidationService;

final class MatchedRoute implements RouteMatch {

   private final IValidationService validator;
   private final Object[] constructorArgs;
   private final Class<?> resourceClass;
   private final Object[] methodArgs;
   private final boolean hasContent;
   private final Method method;

   @Override
   public Class<?> getResourceClass() {
      return resourceClass;
   }

   @Override
   public Method getMethod() {
      return method;
   }

   @Override
   public Object invoke() {
      Object resource = createResource();

      ErrorSet errors = validator.validateParameters(resource, method, methodArgs);
      if (!errors.isEmpty()) {
         throw new BadRequestException(errors);
      }

      Object result = invokeMethod(resource);
      errors = validator.validateReturnValue(resource, method, result);
      if (!errors.isEmpty()) {
         throw new BadResponseException(errors);
      }
      return result;
   }

   @Override
   public Object getContentObject() {
      return hasContent ? methodArgs[0] : null;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof MatchedRoute) {
         MatchedRoute other = (MatchedRoute) obj;
         Object[] thisMethodArgs = Arrays.copyOfRange(methodArgs, hasContent ? 1 : 0,
                  methodArgs.length);
         Object[] otherMethodArgs = Arrays.copyOfRange(other.methodArgs, other.hasContent ? 1 : 0,
                  methodArgs.length);
         if (resourceClass == other.resourceClass && method.equals(other.method)
                  && Arrays.equals(constructorArgs, other.constructorArgs)
                  && Arrays.equals(thisMethodArgs, otherMethodArgs)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      result.append(resourceClass.getName());
      result.append('(');
      result.append(Arrays.stream(constructorArgs).map(Object::toString).collect(
               Collectors.joining(", ")));
      result.append(").");
      result.append(method.getName());
      result.append('(');
      result.append(
               Arrays.stream(methodArgs).map(Object::toString).collect(Collectors.joining(", ")));
      result.append(')');
      return result.toString();
   }

   MatchedRoute(IValidationService validator, MethodEntry entry, Object[] args,
      boolean hasContent) {
      this.resourceClass = entry.getResourceClass();
      this.constructorArgs = Arrays.copyOfRange(args, 0, entry.getResourceArgumentCount());
      this.method = entry.getMethod();
      this.methodArgs = Arrays.copyOfRange(args, entry.getResourceArgumentCount(), args.length);
      this.hasContent = hasContent;
      this.validator = validator;
   }

   Object[] getConstructorArgs() {
      return constructorArgs;
   }

   Object[] getMethodArgs() {
      return methodArgs;
   }

   private Object createResource() {
      Constructor<?> constructor = resourceClass.getConstructors()[0];
      try {
         return constructor.newInstance(constructorArgs);
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

   private Object invokeMethod(Object resource) {
      try {
         return method.invoke(resource, methodArgs);
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof HttpException) {
            throw (HttpException) e.getCause();
         }
         if (e.getCause() instanceof InternalServerError) {
            throw (InternalServerError) e.getCause();
         }
         throw new InternalServerError(e.getCause());
      } catch (IllegalArgumentException e) {
         throw new BadRequestException(e);
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }
}
