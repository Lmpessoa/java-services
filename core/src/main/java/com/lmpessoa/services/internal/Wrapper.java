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
package com.lmpessoa.services.internal;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.lmpessoa.services.concurrent.IAsyncOptions;
import com.lmpessoa.services.concurrent.IExecutionService;
import com.lmpessoa.services.hosting.HeaderMap;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.IApplicationInfo;
import com.lmpessoa.services.hosting.IApplicationOptions;
import com.lmpessoa.services.internal.routing.RouteEntry;
import com.lmpessoa.services.internal.services.NoSingleMethodException;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.routing.HttpMethod;
import com.lmpessoa.services.routing.IRouteOptions;
import com.lmpessoa.services.routing.IRouteTable;
import com.lmpessoa.services.security.IIdentityOptions;
import com.lmpessoa.services.security.IIdentityProvider;
import com.lmpessoa.services.services.HealthStatus;
import com.lmpessoa.services.validating.ErrorSet;
import com.lmpessoa.services.validating.IValidationService;

// The issue with wrappers is that every method in the wrapped interface has to be overridden,
// including defaults because we have no idea when a default method will be overridden in the
// wrapped class.
public final class Wrapper {

   public static ILogger wrap(ILogger original) {
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

   public static IApplicationOptions wrap(IApplicationOptions original) {
      Objects.requireNonNull(original);
      return new IApplicationOptions() {

         @Override
         public void useResponder(Class<?> responderClass) {
            original.useResponder(responderClass);
         }

         @Override
         public void useRoutesWith(Consumer<IRouteOptions> options) {
            original.useRoutesWith(options);
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
         public <T> void useService(Class<T> serviceClass, T instance) {
            original.useService(serviceClass, instance);
         }

         @Override
         public <T> void useServiceWith(Class<T> serviceClass, Supplier<T> supplier) {
            original.useServiceWith(serviceClass, supplier);
         }

         @Override
         public void useStaticFilesAtPath(String staticPath) {
            original.useStaticFilesAtPath(staticPath);
         }

         @Override
         public void useAsyncWith(Consumer<IAsyncOptions> options) {
            original.useAsyncWith(options);
         }

         @Override
         public void useIdentityWith(Class<? extends IIdentityProvider> identityProvider,
            Consumer<IIdentityOptions> options) throws NoSingleMethodException {
            original.useIdentityWith(identityProvider, options);
         }

         @Override
         public void useHealthAtPath(String healthPath) {
            original.useHealthAtPath(healthPath);
         }

         @Override
         public void useXmlRequests() {
            original.useXmlRequests();
         }
      };
   }

   public static IExecutionService wrap(IExecutionService original) {
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

   public static HttpRequest wrap(HttpRequest original) {
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
         public InputStream getContentBody() {
            return original.getContentBody();
         }

         @Override
         public HeaderMap getHeaders() {
            return original.getHeaders();
         }

         @Override
         public Locale[] getAcceptedLanguages() {
            return original.getAcceptedLanguages();
         }
      };
   }

   public static IRouteTable wrap(IRouteTable original) {
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

   public static IValidationService wrap(IValidationService original) {
      Objects.requireNonNull(original);
      return new IValidationService() {

         @Override
         public ErrorSet validate(Object object) {
            return original.validate(object);
         }

         @Override
         public ErrorSet validateParameters(Object object, Method method, Object[] paramValues) {
            return original.validateParameters(object, method, paramValues);
         }

         @Override
         public ErrorSet validateReturnValue(Object object, Method method, Object returnValue) {
            return original.validateReturnValue(object, method, returnValue);
         }
      };
   }

   public static IApplicationInfo wrap(IApplicationInfo original) {
      Objects.requireNonNull(original);
      return new IApplicationInfo() {

         @Override
         public Class<?> getStartupClass() {
            return original.getStartupClass();
         }

         @Override
         public String getName() {
            return original.getName();
         }

         @Override
         public HealthStatus getHealth() {
            return original.getHealth();
         }

         @Override
         public Map<Class<?>, HealthStatus> getServiceHealth() {
            return original.getServiceHealth();
         }

         @Override
         public long getUptime() {
            return original.getUptime();
         }

         @Override
         public long getUsedMemory() {
            return original.getUsedMemory();
         }
      };
   }

   private Wrapper() {
      // Does nothing
   }
}
