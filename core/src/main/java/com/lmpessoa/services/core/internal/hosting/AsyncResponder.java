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
package com.lmpessoa.services.core.internal.hosting;

import static com.lmpessoa.services.core.routing.HttpMethod.DELETE;
import static com.lmpessoa.services.core.routing.HttpMethod.GET;

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

import com.lmpessoa.services.core.ForbiddenException;
import com.lmpessoa.services.core.MethodNotAllowedException;
import com.lmpessoa.services.core.NotFoundException;
import com.lmpessoa.services.core.UnauthorizedException;
import com.lmpessoa.services.core.concurrent.Async;
import com.lmpessoa.services.core.concurrent.AsyncReject;
import com.lmpessoa.services.core.concurrent.AsyncRequest;
import com.lmpessoa.services.core.concurrent.IAsyncRequestMatcher;
import com.lmpessoa.services.core.concurrent.NotAsync;
import com.lmpessoa.services.core.hosting.ConnectionInfo;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.internal.concurrent.DefaultRequestMatcher;
import com.lmpessoa.services.core.internal.concurrent.ExecutionService;
import com.lmpessoa.services.core.internal.concurrent.RejectRequestMatcher;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.security.IIdentity;

final class AsyncResponder {

   private static ExecutionService executor;

   private final Map<UUID, AsyncRequest> routes = new HashMap<>();
   private final ApplicationOptions options;
   private final NextResponder next;

   public AsyncResponder(NextResponder next, ApplicationOptions options) {
      this.options = options;
      this.next = next;
   }

   public static void setExecutor(ExecutionService executor) {
      AsyncResponder.executor = Objects.requireNonNull(executor);
   }

   public Object invoke(HttpRequest request, RouteMatch route, IIdentity identity,
      ConnectionInfo connect) {
      if (executor != null) {
         final String feedbackPath = options.getFeedbakcPath();
         if (request.getPath().startsWith(feedbackPath)) {
            return respondToStatusRequest(request, feedbackPath, identity, connect);
         }
         if (route != null && !(route instanceof HttpException)) {
            IAsyncRequestMatcher rule = getRouteMatcher(route);
            if (rule != null) {
               UUID id = rule.match(new AsyncRequestImpl(identity, route), routes);
               if (id != null) {
                  return RedirectImpl.accepted(feedbackPath + id);
               } else {
                  return respondToAsyncCall(route, identity, feedbackPath);
               }
            }
         }
      }
      return next.invoke();
   }

   private IAsyncRequestMatcher getRouteMatcher(RouteMatch route) {
      if (route.getMethod().isAnnotationPresent(NotAsync.class)) {
         return null;
      }
      Async async = route.getMethod().getAnnotation(Async.class);
      if (async == null && route.getResourceClass() != null) {
         async = route.getResourceClass().getAnnotation(Async.class);
      }
      if (async == null && isCallableResult(route)) {
         return new RejectRequestMatcher(options.getDefaultRejectRule());
      } else if (async != null) {
         Class<? extends IAsyncRequestMatcher> matcherClass = async.rejectWith();
         if (matcherClass == DefaultRequestMatcher.class
                  && options.getDefaultRouteMatcher() != null) {
            matcherClass = options.getDefaultRouteMatcher();
         }
         if (matcherClass == RejectRequestMatcher.class) {
            AsyncReject rule = async.reject();
            if (rule == AsyncReject.DEFAULT) {
               rule = options.getDefaultRejectRule();
            }
            return new RejectRequestMatcher(rule);
         } else {
            try {
               return matcherClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
               throw new InternalServerError(e);
            }
         }
      }
      return null;
   }

   private Object respondToAsyncCall(RouteMatch route, IIdentity identity, String asyncPath) {
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
      UUID id = UUID.fromString(executor.submit(job));
      routes.put(id, new AsyncRequestImpl(identity, route));
      return RedirectImpl.accepted(asyncPath + id);
   }

   private Object respondToStatusRequest(HttpRequest request, String asyncPath, IIdentity identity,
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
            if (obj instanceof RedirectImpl) {
               obj = ((RedirectImpl) obj).getUrl(connect);
            }
            if (obj instanceof URL) {
               return RedirectImpl.seeOther((URL) obj);
            }
         } catch (ExecutionException | MalformedURLException e) {
            // Ignore and return the very same result
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      } else if (method == DELETE) {
         IIdentity requester = routes.get(id).getIdentity();
         if (requester != null) {
            if (identity == null) {
               throw new UnauthorizedException();
            }
            if (!requester.equals(identity)) {
               throw new ForbiddenException();
            }
         }
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

   private static class AsyncRequestImpl implements AsyncRequest {

      private final IIdentity identity;
      private final RouteMatch route;

      public AsyncRequestImpl(IIdentity identity, RouteMatch route) {
         this.identity = identity;
         this.route = route;
      }

      @Override
      public IIdentity getIdentity() {
         return identity;
      }

      @Override
      public RouteMatch getRoute() {
         return route;
      }
   }
}
