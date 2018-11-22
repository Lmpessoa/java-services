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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.security.IIdentityOptions;
import com.lmpessoa.services.core.security.IIdentityProvider;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;

final class ApplicationOptions implements IApplicationOptions {

   private static final String INVALID_PATH = "Given path is not valid";
   private static final String SEPARATOR = "/";

   private final List<Class<?>> responders = new ArrayList<>();
   private final ServiceMap services = new ServiceMap();
   private final RouteTable routes = new RouteTable(services);

   private IIdentityProvider identityProvider = null;
   private boolean configured = false;
   private String feedbackPath;
   private String staticPath;
   private String healthPath;

   // Responder

   @Override
   public void useResponder(Class<?> responderClass) {
      lockConfiguration();
      Objects.requireNonNull(responderClass);
      if (responders.contains(responderClass)) {
         throw new IllegalArgumentException("Responder is already registered");
      }
      if (!ClassUtils.isConcreteClass(responderClass)) {
         throw new IllegalArgumentException("Responder must be a concrete class");
      }
      if (ClassUtils.getConstructor(responderClass, NextResponder.class) == null) {
         throw new IllegalArgumentException("Responder must implement a required constructor");
      }
      Method[] invokes = ClassUtils.findMethods(responderClass, m -> "invoke".equals(m.getName()));
      if (invokes.length != 1) {
         throw new IllegalArgumentException(
                  "Responder must have exaclty one method named 'invoke'");
      }
      responders.add(responderClass);
   }

   // Routes

   @Override
   public void useRoutes(Consumer<IRouteOptions> options) {
      lockConfiguration();
      if (options != null) {
         RouteOptionsImpl impl = new RouteOptionsImpl(routes.getOptions());
         options.accept(routes.getOptions());
         impl.options = null;
      }
   }

   // Services

   @Override
   public <T> void useService(Class<T> serviceClass) {
      lockConfiguration();
      services.put(serviceClass);
   }

   @Override
   public <T> void useService(Class<T> serviceClass, Class<? extends T> implementationClass) {
      lockConfiguration();
      services.put(serviceClass, implementationClass);
   }

   @Override
   public <T> void useService(Class<T> serviceClass, Supplier<T> supplier) {
      lockConfiguration();
      services.put(serviceClass, supplier);
   }

   @Override
   public <T> void useService(Class<T> serviceClass, T instance) {
      lockConfiguration();
      services.put(serviceClass, instance);
   }

   // Async

   @Override
   public void useAsyncWithFeedbackPath(String feedbackPath) {
      lockConfiguration();
      Objects.requireNonNull(feedbackPath);
      if (this.feedbackPath != null) {
         throw new IllegalStateException("Async is already configured");
      }
      if (!feedbackPath.startsWith(SEPARATOR)) {
         feedbackPath = SEPARATOR + feedbackPath;
      }
      if (!feedbackPath.endsWith(SEPARATOR)) {
         feedbackPath += SEPARATOR;
      }
      if (!feedbackPath.matches("/([a-zA-Z0-9.-_]+/)+")) {
         throw new IllegalArgumentException(INVALID_PATH);
      }
      this.feedbackPath = feedbackPath;
   }

   // Static files

   @Override
   public void useStaticFilesAtPath(String staticPath) {
      lockConfiguration();
      Objects.requireNonNull(staticPath);
      if (this.staticPath != null) {
         throw new IllegalStateException("Static files is already configured");
      }
      if (!staticPath.startsWith(SEPARATOR)) {
         staticPath = SEPARATOR + staticPath;
      }
      while (staticPath.endsWith(SEPARATOR)) {
         staticPath = staticPath.substring(0, staticPath.length() - 1);
      }
      if (!staticPath.matches("(/[a-zA-Z0-9.-_]+)+")) {
         throw new IllegalArgumentException(INVALID_PATH);
      }
      this.staticPath = staticPath;
   }

   // Identity

   @Override
   public void useIdentityWith(IIdentityProvider identityProvider,
      Consumer<IIdentityOptions> options) {
      lockConfiguration();
      Objects.requireNonNull(identityProvider);
      if (this.identityProvider != null) {
         throw new IllegalStateException("Identity is already configured");
      }
      this.identityProvider = identityProvider;
      if (options != null) {
         options.accept((policyName, policyMethod) -> {
            lockConfiguration();
            if (IdentityResponder.hasPolicy(policyName)) {
               throw new IllegalArgumentException(
                        "A policy with that name is already defined: " + policyName);
            }
            IdentityResponder.addPolicy(policyName, policyMethod);
         });
      }
   }

   // Health

   @Override
   public void useHealthAtPath(String healthPath) {
      lockConfiguration();
      Objects.requireNonNull(healthPath);
      if (this.healthPath != null) {
         throw new IllegalStateException("Health is already configured");
      }
      if (!healthPath.startsWith(SEPARATOR)) {
         healthPath = SEPARATOR + healthPath;
      }
      if (healthPath.endsWith(SEPARATOR)) {
         healthPath = healthPath.substring(0, healthPath.length() - 1);
      }
      if (!healthPath.matches("(/[a-zA-Z0-9.-_]+)+")) {
         throw new IllegalArgumentException(INVALID_PATH);
      }
      this.healthPath = healthPath;
   }

   ApplicationOptions(Consumer<ServiceMap> configuration) {
      if (configuration != null) {
         configuration.accept(services);
      }
   }

   void configurationEnded() {
      configured = true;
   }

   ServiceMap getServices() {
      return services;
   }

   RouteTable getRoutes() {
      return routes;
   }

   IIdentityProvider getIdentityProvider() {
      return identityProvider;
   }

   String getFeedbakcPath() {
      return feedbackPath;
   }

   String getStaticPath() {
      return staticPath;
   }

   String getHealthPath() {
      return healthPath;
   }

   NextResponder getFirstResponder() {
      List<Class<?>> result = new ArrayList<>();
      result.add(SerializerResponder.class);
      if (healthPath != null) {
         result.add(HealthResponder.class);
      }
      if (staticPath != null) {
         result.add(StaticResponder.class);
      }
      result.add(FaviconResponder.class);

      result.addAll(this.responders);

      if (identityProvider != null) {
         result.add(IdentityResponder.class);
      }
      if (feedbackPath != null) {
         result.add(AsyncResponder.class);
      }
      result.add(InvokeResponder.class);
      return new NextResponderImpl(services, result, this);
   }

   private void lockConfiguration() {
      if (configured) {
         throw new IllegalStateException("Application is already configured");
      }
   }

   private class RouteOptionsImpl implements IRouteOptions {

      private IRouteOptions options;

      public RouteOptionsImpl(IRouteOptions options) {
         this.options = options;
      }

      @Override
      public void addArea(String areaPath, String packageExpr) {
         lockConfiguration();
         options.addArea(areaPath, packageExpr);
      }

      @Override
      public void addArea(String areaPath, String packageExpr, String defaultResource) {
         lockConfiguration();
         options.addArea(areaPath, packageExpr, defaultResource);
      }
   }
}
