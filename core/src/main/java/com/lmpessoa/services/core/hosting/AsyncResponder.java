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

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.lmpessoa.services.core.concurrent.Async;
import com.lmpessoa.services.core.concurrent.ExecutionService;
import com.lmpessoa.services.core.routing.RouteMatch;

final class AsyncResponder {

   private static ExecutionService executor;
   private static String feedbackPath;

   private final NextResponder next;

   public AsyncResponder(NextResponder next) {
      this.next = next;
   }

   public static void setExecutor(ExecutionService executor) {
      AsyncResponder.executor = Objects.requireNonNull(executor);
   }

   public Object invoke(HttpRequest request, RouteMatch route) {
      if (executor != null) {
         if (request.getPath().startsWith(feedbackPath)) {
            return respondToStatusRequest(request, feedbackPath);
         }
         if (route != null && (isCallableResult(route) || isAsync(route))) {
            return respondToAsyncCall(route, feedbackPath);
         }
      }
      return next.invoke();
   }

   static String getFeedbackPath() {
      return feedbackPath;
   }

   static void setFeedbackPath(String feedbackPath) {
      AsyncResponder.feedbackPath = feedbackPath;
   }

   private Object respondToAsyncCall(RouteMatch route, String asyncPath) {
      Callable<?> job = null;
      if (isCallableResult(route)) {
         Object result = next.invoke();
         if (result instanceof Callable) {
            job = (Callable<?>) result;
         } else if (result instanceof Runnable) {
            job = Executors.callable((Runnable) result);
         }
      } else if (isAsync(route)) {
         job = route::invoke;
      }
      String id = executor.submit(job);
      return Redirect.accepted(asyncPath + id);
   }

   private Object respondToStatusRequest(HttpRequest request, String asyncPath) {
      UUID id;
      try {
         id = UUID.fromString(request.getPath().substring(asyncPath.length()));
      } catch (IllegalArgumentException e) {
         throw new NotFoundException();
      }
      Future<?> result = executor.get(id.toString());
      if (result == null) {
         throw new NotFoundException();
      }
      return result;
   }

   private boolean isAsync(RouteMatch match) {
      Method method = match.getMethod();
      Class<?> clazz = match.getResourceClass();
      return method != null && method.isAnnotationPresent(Async.class)
               || clazz != null && clazz.isAnnotationPresent(Async.class);
   }

   private boolean isCallableResult(RouteMatch match) {
      Method method = match.getMethod();
      if (method == null) {
         return false;
      }
      Class<?> result = method.getReturnType();
      return result == Callable.class || result == Runnable.class;
   }
}
