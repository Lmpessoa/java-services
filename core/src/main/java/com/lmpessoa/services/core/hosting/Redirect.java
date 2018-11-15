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
package com.lmpessoa.services.core.hosting;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.util.ConnectionInfo;

/**
 * Represents a redirection of the client to another location.
 *
 * <p>
 * In the HTTP protocol, redirections indicate that further action needs to be taken by the user
 * agent in order to fulfill the request. The action required may be carried out by the user agent
 * without interaction with the user.
 * </p>
 *
 * <p>
 * Redirection objects cannot be modified once created and must be returned by the resource method
 * to be effectively sent as response to a request.
 * </p>
 */
public final class Redirect {

   private static RouteTable routes; // NOSONAR

   private final Parameter params;
   private final int status;

   // 201 Created

   /**
    * Returns a redirection that indicates a resource was successfully created at the given
    * location.
    *
    * <p>
    * The value of this location may be a root path (beginning with a '/') instead of a full URL. In
    * this case, the given path is assumed to be relative to the current application and a full URL
    * will be sent to the client using the current server information.
    * </p>
    *
    * @param url the location of the created resource.
    * @return an object representing this redirection.
    */
   public static Redirect createdAt(String url) {
      return new Redirect(201, new PathParameter(url));
   }

   /**
    * Returns a redirection that indicates a resource was successfully created at the given
    * location.
    *
    * @param url the location of the created resource.
    * @return an object representing this redirection.
    */
   public static Redirect createdAt(URL url) {
      return new Redirect(201, new PathParameter(url.toExternalForm()));
   }

   /**
    * Returns a redirection that indicates a resource was successfully created at the given
    * location.
    *
    * <p>
    * Redirections created through this method will return an URL on the current application where
    * the given method of the given class will be called with the given arguments. Note that the
    * appointed method may not be published by the application (or even not exist at all) and thus
    * have no URL.
    * </p>
    *
    * @param clazz the class which contains the method with the given name.
    * @param method the name of the method in the given class to redirect to.
    * @param args the list of arguments to be used with the given method.
    * @return an object representing this redirection.
    */
   public static Redirect createdAt(Class<?> clazz, String method, Object... args) {
      return new Redirect(201, new ActionParameter(clazz, method, args));
   }

   // 302 Found

   /**
    * Returns a redirection to the given location.
    *
    * <p>
    * The value of this location may be a root path (beginning in '/') instead of a full URL. In
    * this case, the given path is assumed to be relative to the current application and a full URL
    * will be sent to the client using the current server information.
    * </p>
    *
    * @param url the location the client should redirect to.
    * @return an object representing this redirection.
    */
   public static Redirect to(String url) {
      return new Redirect(302, new PathParameter(url));
   }

   /**
    * Returns a redirection to the given location.
    *
    * @param url the location the client should redirect to.
    * @return an object representing this redirection.
    */
   public static Redirect to(URL url) {
      return new Redirect(302, new PathParameter(url.toExternalForm()));
   }

   /**
    * Returns a redirection to the given location.
    *
    * <p>
    * Redirections created through this method will return an URL on the current application where
    * the given method of the given class will be called with the given arguments. Note that the
    * appointed method may not be published by the application (or even not exist at all) and thus
    * have no URL.
    * </p>
    *
    * @param clazz the class which contains the method with the given name.
    * @param method the name of the method in the given class to redirect to.
    * @param args the list of arguments to be used with the given method.
    * @return an object representing this redirection.
    */
   public static Redirect to(Class<?> clazz, String method, Object... args) {
      return new Redirect(302, new ActionParameter(clazz, method, args));
   }

   // 307 Temporary Redirect

   /**
    * Returns a temporary redirection to the given location.
    *
    * <p>
    * The value of this location may be a root path (beginning in '/') instead of a full URL. In
    * this case, the given path is assumed to be relative to the current application and a full URL
    * will be sent to the client using the current server information.
    * </p>
    *
    * @param url the location the client should redirect to.
    * @return an object representing this redirection.
    */
   public static Redirect temporaryTo(String url) {
      return new Redirect(307, new PathParameter(url));
   }

   /**
    * Returns a temporary redirection to the given location.
    *
    * @param url the location the client should redirect to.
    * @return an object representing this redirection.
    */
   public static Redirect temporaryTo(URL url) {
      return new Redirect(307, new PathParameter(url.toExternalForm()));
   }

   /**
    * Returns a temporary redirection to the given location.
    *
    * <p>
    * Redirections created through this method will return an URL on the current application where
    * the given method of the given class will be called with the given arguments. Note that the
    * appointed method may not be published by the application (or even not exist at all) and thus
    * have no URL.
    * </p>
    *
    * @param clazz the class which contains the method with the given name.
    * @param method the name of the method in the given class to redirect to.
    * @param args the list of arguments to be used with the given method.
    * @return an object representing this redirection.
    */
   public static Redirect temporaryTo(Class<?> clazz, String method, Object... args) {
      return new Redirect(307, new ActionParameter(clazz, method, args));
   }

   // 308 Permanent Redirect

   /**
    * Returns a permanent redirection to the given location.
    *
    * <p>
    * The value of this location may be a root path (beginning in '/') instead of a full URL. In
    * this case, the given path is assumed to be relative to the current application and a full URL
    * will be sent to the client using the current server information.
    * </p>
    *
    * @param url the location the client should redirect to.
    * @return an object representing this redirection.
    */
   public static Redirect permanentTo(String url) {
      return new Redirect(308, new PathParameter(url));
   }

   /**
    * Returns a permanent redirection to the given location.
    *
    * @param url the location the client should redirect to.
    * @return an object representing this redirection.
    */
   public static Redirect permanentTo(URL url) {
      return new Redirect(308, new PathParameter(url.toExternalForm()));
   }

   /**
    * Returns a permanent redirection to the given location.
    *
    * <p>
    * Redirections created through this method will return an URL on the current application where
    * the given method of the given class will be called with the given arguments. Note that the
    * appointed method may not be published by the application (or even not exist at all) and thus
    * have no URL.
    * </p>
    *
    * @param clazz the class which contains the method with the given name.
    * @param method the name of the method in the given class to redirect to.
    * @param args the list of arguments to be used with the given method.
    * @return an object representing this redirection.
    */
   public static Redirect permanentTo(Class<?> clazz, String method, Object... args) {
      return new Redirect(308, new ActionParameter(clazz, method, args));
   }

   @Override
   public String toString() {
      return String.format("%s [%d]", params.getPath(), status);
   }

   int getStatusCode() {
      return status;
   }

   static void setRoutes(RouteTable routes) {
      Redirect.routes = routes;
   }

   static Redirect accepted(String url) {
      return new Redirect(202, new PathParameter(url));
   }

   URL getUrl(ConnectionInfo info) throws MalformedURLException {
      String result = params.getPath();
      if (result == null) {
         throw new MalformedURLException("Cannot find path to resource");
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

   private Redirect(int status, Parameter params) {
      this.status = status;
      this.params = params;
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
