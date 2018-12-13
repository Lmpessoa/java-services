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
package com.lmpessoa.services;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.internal.hosting.HttpException;
import com.lmpessoa.services.internal.serializing.Serializer;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.validating.ErrorSet;

/**
 * Thrown when the received request is not valid.
 */
public final class BadRequestException extends HttpException implements RouteMatch {

   private static final long serialVersionUID = 1L;

   private final Class<?> resourceClass;
   private final ErrorSet errors;
   private final Method method;

   private HttpRequest request;

   /**
    * Creates a new {@code BadRequestException}.
    */
   public BadRequestException() {
      this(null, null, (String) null);
   }

   /**
    * Creates a new {@code BadRequestException}.
    *
    * <p>
    * By providing a resource class and method the exception indicates a problem regarding the
    * arguments of the method (i.e. values incompatible with the actual type of the parameters).
    * </p>
    *
    * @param resourceClass the resource class that contains the method.
    * @param method the method called with incompatible arguments.
    */
   public BadRequestException(Class<?> resourceClass, Method method) {
      this(resourceClass, method, (String) null);
   }

   /**
    * Creates a new {@code BadRequestException} with the given detail message.
    *
    * <p>
    * By providing a resource class and method the exception indicates a problem regarding the
    * arguments of the method (i.e. values incompatible with the actual type of the parameters).
    * </p>
    *
    * @param resourceClass the resource class that contains the method.
    * @param method the method called with incompatible arguments.
    * @param message the detail message.
    */
   public BadRequestException(Class<?> resourceClass, Method method, String message) {
      super(400, message);
      this.resourceClass = resourceClass;
      this.method = method;
      this.errors = null;
   }

   /**
    * Creates a new {@code BadRequestException} with the given detail messages.
    *
    * <p>
    * By providing a resource class and method the exception indicates a problem regarding the
    * arguments of the method (i.e. values incompatible with the actual type of the parameters).
    * </p>
    *
    * @param resourceClass the resource class that contains the method.
    * @param method the method called with incompatible arguments.
    * @param errors the detail messages.
    */
   public BadRequestException(Class<?> resourceClass, Method method, ErrorSet errors) {
      super(400);
      this.resourceClass = resourceClass;
      this.method = method;
      this.errors = errors;
   }

   /**
    * Creates a new {@code BadRequestException} with the given cause.
    *
    * <p>
    * By providing a resource class and method the exception indicates a problem regarding the
    * arguments of the method (i.e. values incompatible with the actual type of the parameters).
    * </p>
    *
    * @param resourceClass the resource class that contains the method.
    * @param method the method called with incompatible arguments.
    * @param cause the cause for the exception.
    */
   public BadRequestException(Class<?> resourceClass, Method method, Throwable t) {
      super(400, t);
      this.resourceClass = resourceClass;
      this.method = method;
      this.errors = null;
   }

   /**
    * Returns a set of errors describing the problems with the current request.
    *
    * @return a set of errors describing the problems with the current request, or {@code null} if
    *         no such set is present.
    */
   public ErrorSet getErrors() {
      return errors;
   }

   public void setRequest(HttpRequest request) {
      this.request = request;
   }

   @Override
   public HttpInputStream getContentBody() {
      if (errors != null && request != null) {
         final List<String> accepts = new ArrayList<>();
         if (request.getHeaders().contains(Headers.ACCEPT)) {
            Arrays.stream(request.getHeaders().getAll(Headers.ACCEPT))
                     .map(s -> s.split(","))
                     .forEach(s -> Arrays.stream(s).map(ss -> ss.split(";")[0].trim()).forEach(
                              accepts::add));
            if (!accepts.contains(ContentType.JSON) && accepts.contains("*/*")) {
               accepts.set(accepts.indexOf("*/*"), ContentType.JSON);
            }
         } else {
            accepts.add(ContentType.JSON);
         }
         return Serializer.fromObject(errors, accepts.toArray(new String[0]),
                  request.getAcceptedLanguages());
      }
      return new HttpInputStream(getMessage().getBytes(UTF_8), ContentType.TEXT, UTF_8);
   }

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
      throw this;
   }
}
