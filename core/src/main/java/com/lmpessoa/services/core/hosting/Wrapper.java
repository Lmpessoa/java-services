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
import java.util.function.Supplier;

import com.lmpessoa.services.core.concurrent.IExecutionService;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.util.logging.ILogger;

// The issue with wrappers is that every method in the wrapped interface has to be overridden,
// including defaults because we have no idea when a default method will be overridden in the
// wrapped class.
final class Wrapper {

   static IServiceMap wrap(IServiceMap original) {
      Objects.requireNonNull(original);
      return new IServiceMap() {

         @Override
         public <T> void put(Class<T> service, Supplier<T> supplier) {
            original.put(service, supplier);
         }

         @Override
         public <T> void put(Class<T> service, T instance) {
            original.put(service, instance);
         }
      };
   }

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

   static IApplicationInfo wrap(IApplicationInfo original) {
      Objects.requireNonNull(original);
      return original::getStartupClass;
   }

   static IApplicationOptions wrap(IApplicationOptions original) {
      Objects.requireNonNull(original);
      return new IApplicationOptions() {

         @Override
         public void useHandler(Class<?> handlerClass) {
            original.useHandler(handlerClass);
         }

         @Override
         public String getAsyncFeedbackPath() {
            return original.getAsyncFeedbackPath();
         }

         @Override
         public void setAsyncFeedbackPath(String path) {
            original.setAsyncFeedbackPath(path);
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
         public String getMethod() {
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
         public HeaderMap getHeaders() {
            return original.getHeaders();
         }

         @Override
         public String getHeader(String headerName) {
            return original.getHeader(headerName);
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
         public void put(Class<?> clazz) {
            original.put(clazz);
         }

         @Override
         public void put(String area, Class<?> clazz) {
            original.put(area, clazz);
         }

         @Override
         public void putAll(Collection<Class<?>> classes) {
            original.putAll(classes);
         }

         @Override
         public void putAll(String area, Collection<Class<?>> classes) {
            original.putAll(area, classes);
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
