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

import static com.lmpessoa.services.core.routing.HttpMethod.DELETE;
import static com.lmpessoa.services.core.routing.HttpMethod.GET;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.lmpessoa.services.core.concurrent.Async;
import com.lmpessoa.services.core.concurrent.ExecutionService;
import com.lmpessoa.services.core.concurrent.IAsyncRejectionRule;
import com.lmpessoa.services.core.concurrent.NotAsync;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.RouteMatch;

final class AsyncResponder {

   private static ExecutionService executor;

   private final Map<UUID, RouteMatch> routes = new HashMap<>();
   private final ApplicationOptions options;
   private final NextResponder next;

   public AsyncResponder(NextResponder next, ApplicationOptions options) {
      this.options = options;
      this.next = next;
   }

   public static void setExecutor(ExecutionService executor) {
      AsyncResponder.executor = Objects.requireNonNull(executor);
   }

   public Object invoke(HttpRequest request, RouteMatch route, ConnectionInfo connect) {
      if (executor != null) {
         final String feedbackPath = options.getFeedbakcPath();
         if (request.getPath().startsWith(feedbackPath)) {
            return respondToStatusRequest(request, feedbackPath, connect);
         }
         if (route != null && !(route instanceof HttpException)) {
            IAsyncRejectionRule rule = getRejectionRule(route);
            if (rule != null && rule.shouldReject(route, routes.values())) {
               throw new ConflictException("A request for this is already awaiting completion");
            } else if (rule != null || isCallableResult(route)) {
               return respondToAsyncCall(route, feedbackPath);
            }
         }
      }
      return next.invoke();
   }

   private IAsyncRejectionRule getRejectionRule(RouteMatch route) {
      if (route.getMethod().isAnnotationPresent(NotAsync.class)) {
         return null;
      }
      Async async = route.getMethod().getAnnotation(Async.class);
      if (async == null && route.getResourceClass() != null) {
         async = route.getResourceClass().getAnnotation(Async.class);
      }
      try {
         if (async == null) {
            return null;
         }
         Constructor<? extends IAsyncRejectionRule> constr = async.rejectWith()
                  .getConstructor(Async.class);
         constr.setAccessible(true);
         return constr.newInstance(async);
      } catch (InvocationTargetException e) {
         throw new InternalServerError(e.getCause());
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
         throw new InternalServerError(e);
      }
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
      } else {
         job = route::invoke;
      }
      String id = executor.submit(job);
      routes.put(UUID.fromString(id), route);
      return Redirect.accepted(asyncPath + id);
   }

   private Object respondToStatusRequest(HttpRequest request, String asyncPath,
      ConnectionInfo connect) {
      UUID id;
      HttpMethod method = request.getMethod();
      if (method != GET && method != DELETE) {
         throw new MethodNotAllowedException();
      }
      try {
         id = UUID.fromString(request.getPath().substring(asyncPath.length()));
      } catch (IllegalArgumentException e) {
         throw new NotFoundException();
      }
      Future<?> result = executor.get(id.toString());
      if (result == null) {
         throw new NotFoundException();
      }
      if (result.isDone()) {
         routes.remove(id);
         try {
            Object obj = result.get();
            if (obj instanceof Redirect) {
               obj = ((Redirect) obj).getUrl(connect);
            }
            if (obj instanceof URL) {
               return Redirect.seeOther((URL) obj);
            }
         } catch (ExecutionException | MalformedURLException e) {
            // Ignore and return the very same result
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      } else if (method == DELETE) {
         result.cancel(true);
      }
      return result;
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
