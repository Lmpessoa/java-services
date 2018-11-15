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

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.lmpessoa.services.core.concurrent.IExecutionService;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteEntry;
import com.lmpessoa.services.core.security.IIdentityOptions;
import com.lmpessoa.services.core.security.IIdentityProvider;
import com.lmpessoa.services.util.logging.ILogger;

// The issue with wrappers is that every method in the wrapped interface has to be overridden,
// including defaults because we have no idea when a default method will be overridden in the
// wrapped class.
final class Wrapper {

   static ILogger wrap(ILogger original) {
      Objects.requireNonNull(original);
      return new ILogger() {

         @Override
         public void fatal(Object message) {
            original.fatal(message);
         }

         @Override
         public void fatal(String message, Object... args) {
            original.fatal(message, args);
         }

         @Override
         public void error(Object message) {
            original.error(message);
         }

         @Override
         public void error(String message, Object... args) {
            original.error(message, args);
         }

         @Override
         public void warning(Object message) {
            original.warning(message);
         }

         @Override
         public void warning(String message, Object... args) {
            original.warning(message, args);
         }

         @Override
         public void info(Object message) {
            original.info(message);
         }

         @Override
         public void info(String message, Object... args) {
            original.info(message, args);
         }

         @Override
         public void debug(Object message) {
            original.debug(message);
         }

         @Override
         public void debug(String message, Object... args) {
            original.debug(message, args);
         }
      };
   }

   static IApplicationSettings wrap(IApplicationSettings original) {
      Objects.requireNonNull(original);
      return new IApplicationSettings() {

         @Override
         public Class<?> getStartupClass() {
            return original.getStartupClass();
         }

         @Override
         public String getApplicationName() {
            return original.getApplicationName();
         }

         @Override
         public boolean isXmlEnabled() {
            return original.isXmlEnabled();
         }

      };
   }

   static IApplicationOptions wrap(IApplicationOptions original) {
      Objects.requireNonNull(original);
      return new IApplicationOptions() {

         @Override
         public void useResponder(Class<?> responderClass) {
            original.useResponder(responderClass);
         }

         @Override
         public void useRoutes(Consumer<IRouteOptions> options) {
            original.useRoutes(options);
         }

         @Override
         public <T> void useService(Class<T> serviceClass) {
            original.useService(serviceClass);
         }

         @Override
         public <T> void useService(Class<T> serviceClass, Class<? extends T> implementationClass) {
            original.useService(serviceClass, implementationClass);
         }

         @Override
         public <T> void useService(Class<T> serviceClass, Supplier<T> supplier) {
            original.useService(serviceClass, supplier);
         }

         @Override
         public <T> void useService(Class<T> serviceClass, T instance) {
            original.useService(serviceClass, instance);
         }

         @Override
         public void useAsync() {
            original.useAsync();
         }

         @Override
         public void useAsyncWithFeedbackPath(String feedbackPath) {
            original.useAsyncWithFeedbackPath(feedbackPath);
         }

         @Override
         public void useStaticFiles() {
            original.useStaticFiles();
         }

         @Override
         public void useStaticFilesAtPath(String staticPath) {
            original.useStaticFilesAtPath(staticPath);
         }

         @Override
         public void useIdentity(IIdentityProvider identityProvider) {
            original.useIdentity(identityProvider);
         }

         @Override
         public void useIdentity(IIdentityProvider identityProvider,
            Consumer<IIdentityOptions> options) {
            original.useIdentity(identityProvider, options);
         }
      };
   }

   static IExecutionService wrap(IExecutionService original) {
      Objects.requireNonNull(original);
      return new IExecutionService() {

         @Override
         public Future<?> get(String jobId) {
            return original.get(jobId);
         }

         @Override
         public Set<String> keySet() {
            return original.keySet();
         }

         @Override
         public boolean isShutdown() {
            return original.isShutdown();
         }
      };
   }

   static HttpRequest wrap(HttpRequest original) {
      Objects.requireNonNull(original);
      return new HttpRequest() {

         @Override
         public HttpMethod getMethod() {
            return original.getMethod();
         }

         @Override
         public String getPath() {
            return original.getPath();
         }

         @Override
         public String getProtocol() {
            return original.getProtocol();
         }

         @Override
         public String getQueryString() {
            return original.getQueryString();
         }

         @Override
         public long getContentLength() {
            return original.getContentLength();
         }

         @Override
         public String getContentType() {
            return original.getContentType();
         }

         @Override
         public InputStream getBody() {
            return original.getBody();
         }

         @Override
         public String[] getHeaderNames() {
            return original.getHeaderNames();
         }

         @Override
         public String getHeader(String headerName) {
            return original.getHeader(headerName);
         }

         @Override
         public String[] getHeaderValues(String headerName) {
            return original.getHeaderValues(headerName);
         }

         @Override
         public boolean containsHeaders(String headerName) {
            return original.containsHeaders(headerName);
         }

         @Override
         public Map<String, Collection<String>> getQuery() {
            return original.getQuery();
         }
      };
   }

   static IRouteTable wrap(IRouteTable original) {
      Objects.requireNonNull(original);
      return new IRouteTable() {

         @Override
         public Collection<RouteEntry> put(Class<?> clazz) {
            return original.put(clazz);
         }

         @Override
         public Collection<RouteEntry> put(String area, Class<?> clazz) {
            return original.put(area, clazz);
         }

         @Override
         public Collection<RouteEntry> putAll(Collection<Class<?>> classes) {
            return original.putAll(classes);
         }

         @Override
         public Collection<RouteEntry> putAll(String area, Collection<Class<?>> classes) {
            return original.putAll(area, classes);
         }

         @Override
         public String findArea(String packageName) {
            return original.findArea(packageName);
         }
      };
   }

   private Wrapper() {
      // Does nothing
   }
}
