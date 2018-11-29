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
package com.lmpessoa.services.internal.hosting;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import com.lmpessoa.services.Redirect;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.internal.routing.RouteTable;

/**
 * Represents a redirection of the client to another location.
 *
 * <p>
 * In the HTTP protocol, redirections indicate that further action needs to be taken by the user
 * agent in order to fulfll the request. The action required may be carried out by the user agent
 * without interaction with the user.
 * </p>
 *
 * <p>
 * Redirection objects cannot be modified once created and must be returned by the resource method
 * to be effectively sent as response to a request.
 * </p>
 */
public final class RedirectImpl implements Redirect {

   private static RouteTable routes;

   private final Parameter params;
   private final int status;

   public RedirectImpl(int status, String url) {
      this.status = status;
      this.params = new PathParameter(url);
   }

   public RedirectImpl(int status, Class<?> clazz, String methodName, Object... args) {
      this.status = status;
      this.params = new ActionParameter(clazz, methodName, args);
   }

   @Override
   public int getStatusCode() {
      return status;
   }

   @Override
   public String toString() {
      return String.format("%s [%d]", params.getPath(), status);
   }

   static void setRoutes(RouteTable routes) {
      RedirectImpl.routes = routes;
   }

   static Redirect accepted(String url) {
      return new RedirectImpl(202, url);
   }

   static Redirect accepted(URL url) {
      return new RedirectImpl(202, url.toExternalForm());
   }

   static Redirect accepted(Class<?> clazz, String method, Object... args) {
      return new RedirectImpl(202, clazz, method, args);
   }

   static Redirect seeOther(URL url) {
      return new RedirectImpl(303, url.toExternalForm());
   }

   URL getUrl(ConnectionInfo info) throws MalformedURLException {
      String result = params.getPath();
      if (result == null) {
         throw new MalformedURLException(CoreMessage.PATH_MISSING.get());
      }
      if (result.startsWith("/") && info != null) {
         int port = info.getServerPort();
         boolean secure = info.isSecure();
         int defaultPort = secure ? 443 : 80;
         StringBuilder resultUrl = new StringBuilder();
         resultUrl.append("http");
         if (secure) {
            resultUrl.append('s');
         }
         resultUrl.append("://");
         resultUrl.append(info.getServerName());
         if (port != defaultPort) {
            resultUrl.append(':');
            resultUrl.append(port);
         }
         resultUrl.append(result);
         result = resultUrl.toString();
      }
      return new URL(result);
   }

   private static interface Parameter {

      String getPath();
   }

   private static class PathParameter implements Parameter {

      private final String path;

      public PathParameter(String path) {
         this.path = Objects.requireNonNull(path);
      }

      @Override
      public String getPath() {
         return path;
      }
   }

   private static class ActionParameter implements Parameter {

      private final Class<?> clazz;
      private final String method;
      private final Object[] args;

      public ActionParameter(Class<?> clazz, String method, Object[] args) {
         this.clazz = Objects.requireNonNull(clazz);
         this.method = Objects.requireNonNull(method);
         this.args = args;
      }

      @Override
      public String getPath() {
         return routes.findPathTo(clazz, method, args);
      }
   }
}
