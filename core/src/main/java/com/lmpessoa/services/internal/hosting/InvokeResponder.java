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
package com.lmpessoa.services.internal.hosting;

import java.lang.reflect.Method;

import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.hosting.NextResponder;
import com.lmpessoa.services.internal.routing.BadResponseException;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.validating.ErrorSet;

final class InvokeResponder {

   public InvokeResponder(NextResponder next) {
      // Last handler, no need for next
   }

   public Object invoke(RouteMatch route, ILogger log) {
      if (route == null) {
         throw new NotFoundException();
      }
      try {
         return route.invoke();
      } catch (BadResponseException e) {
         e.getErrors().stream().map(err -> new ErrorMessage(err, route.getMethod())).forEach(
                  log::error);
         throw new InternalServerError("Application produced an unexpected result");
      }
   }

   private static class ErrorMessage extends Throwable {

      private static final long serialVersionUID = 1L;
      private final ErrorSet.Entry error;
      private final Method method;

      public ErrorMessage(ErrorSet.Entry error, Method method) {
         this.method = method;
         this.error = error;
      }

      @Override
      public synchronized Throwable fillInStackTrace() {
         return this;
      }

      @Override
      public String getMessage() {
         return String.format("%s: %s (was '%s')", error.getPathEntry(), error.getMessage(),
                  error.getInvalidValue());
      }

      @Override
      public StackTraceElement[] getStackTrace() {
         return new StackTraceElement[] { new StackTraceElement(
                  method.getDeclaringClass().getName(), method.getName(), null, 0) };
      }
   }
}
